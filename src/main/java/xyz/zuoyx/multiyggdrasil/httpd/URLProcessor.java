/*
 * Copyright (C) 2022  Haowei Wen <yushijinhun@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package xyz.zuoyx.multiyggdrasil.httpd;

import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_TEXT;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asBytes;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.sendResponse;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.transfer;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.DEBUG;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.INFO;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.WARNING;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import xyz.zuoyx.multiyggdrasil.Config;
import xyz.zuoyx.multiyggdrasil.util.UnsupportedURLException;

public class URLProcessor {

	private static final Pattern URL_REGEX = Pattern.compile("^(?<protocol>https?):\\/\\/(?<domain>[^\\/]+)(?<path>\\/?.*)$");
	private static final Pattern LOCAL_URL_REGEX = Pattern.compile("^/(?<protocol>https?)/(?<domain>[^\\/]+)(?<path>\\/.*)$");

	private List<URLFilter> filters;
	private URLRedirector redirector;

	public URLProcessor(List<URLFilter> filters, URLRedirector redirector) {
		this.filters = filters;
		this.redirector = redirector;
	}

	/**
	 * Transforms the input URL(which is grabbed from the bytecode).
	 * <p>
	 * If any filter is interested in the URL, the URL will be redirected to the local HTTP server.
	 * Otherwise, the URLRedirector will be invoked to determine whether the URL should be modified
	 * and pointed to the customized authentication server.
	 * If none of above happens, empty is returned.
	 *
	 * @return the transformed URL, or empty if it doesn't need to be transformed
	 */
	public Optional<String> transformURL(String inputUrl) {
		if (!inputUrl.startsWith("http")) {
			// fast path
			return Optional.empty();
		}
		Matcher matcher = URL_REGEX.matcher(inputUrl);
		if (!matcher.find()) {
			return Optional.empty();
		}
		String protocol = matcher.group("protocol");
		String domain = matcher.group("domain");
		String path = matcher.group("path");

		Optional<String> result = transform(protocol, domain, path);
		if (result.isPresent()) {
			log(DEBUG, "Transformed url [" + inputUrl + "] to [" + result.get() + "]");
		}
		return result;
	}

	private Optional<String> transform(String protocol, String domain, String path) {
		boolean handleLocally = false;
		for (URLFilter filter : filters) {
			if (filter.canHandle(domain)) {
				handleLocally = true;
				break;
			}
		}

		if (handleLocally) {
			return Optional.of("http://127.0.0.1:" + getLocalApiPort() + "/" + protocol + "/" + domain + path);
		}

		return redirector.redirect(domain, path);
	}

	private DebugApiEndpoint debugApi = new DebugApiEndpoint();
	private volatile HttpServer httpServer;
	private final Object httpServerLock = new Object();

	private int getLocalApiPort() {
		synchronized (httpServerLock) {
			if (httpServer == null) {
				try {
					httpServer = createHttpServer();
				} catch (IOException e) {
					throw new IllegalStateException("HTTP server failed to create");
				}
				httpServer.setExecutor(Executors.newCachedThreadPool());
				httpServer.start();
				log(INFO, "HTTP server is running on port " + httpServer.getAddress().getPort());
			}
			return httpServer.getAddress().getPort();
		}
	}

	private HttpServer createHttpServer() throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", Config.httpdPort), 0);
		server.createContext("/debug/", exchange -> debugApi.serve(exchange));
		server.createContext("/", exchange -> {
			Matcher matcher = LOCAL_URL_REGEX.matcher(exchange.getRequestURI().getPath());
			if (matcher.find()) {
				String protocol = matcher.group("protocol");
				String domain = matcher.group("domain");
				String path = matcher.group("path");
				for (URLFilter filter : filters) {
					if (filter.canHandle(domain)) {
						try {
							filter.handle(domain, path, exchange);
						} catch (UnsupportedURLException e) {
							continue;
						} catch (Throwable e) {
							log(WARNING, "An error occurred while processing request [" + exchange.getRequestURI().getPath() + "]", e);
							sendResponse(exchange, 500, CONTENT_TYPE_TEXT, "Internal Server Error".getBytes());
							return;
						}

						log(DEBUG, "Request to [" + exchange.getRequestURI().getPath() + "] is handled by [" + filter + "]");
						return;
					}
				}

				String target = redirector.redirect(domain, path)
						.orElseGet(() -> protocol + "://" + domain + path);
				try {
					reverseProxy(exchange, target);
				} catch (URISyntaxException | IOException e) {
					log(WARNING, "Reverse proxy error", e);
					sendResponse(exchange, 502, CONTENT_TYPE_TEXT, "Bad Gateway".getBytes());
				}
			} else {
				log(DEBUG, "No handler is found for [" + exchange.getRequestURI().getPath() + "]");
				sendResponse(exchange, 404, CONTENT_TYPE_TEXT, "Not Found".getBytes());
			}
		});
		return server;
	}

	private static final Set<String> ignoredHeaders = new HashSet<>(Arrays.asList("host", "expect", "connection", "keep-alive", "transfer-encoding"));

	private void reverseProxy(HttpExchange exchange, String upstream) throws URISyntaxException, IOException {
		String method = exchange.getRequestMethod();

		String rawQuery = exchange.getRequestURI().getRawQuery();
		String url = rawQuery == null ? upstream : upstream + "?" + rawQuery;

		Map<String, List<String>> requestHeaders = new LinkedHashMap<>(exchange.getRequestHeaders());
		ignoredHeaders.forEach(requestHeaders::remove);

		InputStream clientIn = exchange.getRequestBody();

		log(DEBUG, "Reverse proxy: > " + method + " " + url + ", headers: " + requestHeaders);

		HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
		conn.setRequestMethod(method);
		conn.setDoOutput(clientIn.available() != 0);
		requestHeaders.forEach((key, values) -> {
			String value = String.join(",", values);
			conn.setRequestProperty(key, value);
		});

		if (clientIn.available() != 0) {
			try (OutputStream upstreamOut = conn.getOutputStream()) {
				transfer(clientIn, upstreamOut);
			}
		}

		int responseCode = conn.getResponseCode();
		String reponseMessage = conn.getResponseMessage();
		Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
		conn.getHeaderFields().forEach((name, values) -> {
			if (name != null && !ignoredHeaders.contains(name.toLowerCase())) {
				responseHeaders.put(name, values);
			}
		});
		InputStream upstreamIn;
		try {
			upstreamIn = conn.getInputStream();
		} catch (IOException e) {
			upstreamIn = conn.getErrorStream();
		}
		log(DEBUG, "Reverse proxy: < " + responseCode + " " + reponseMessage + " , headers: " + responseHeaders);

		// no content
		long contentLength = -1;
		for (Entry<String, List<String>> header : responseHeaders.entrySet()) {
			if ("content-length".equalsIgnoreCase(header.getKey())) {
				contentLength = Long.parseLong(header.getValue().get(0));
				break;
			}
		}

		if (contentLength == -1 && conn.getHeaderField("transfer-encoding") != null) {
			// chunked encoding
			contentLength = 0;
		}
		responseHeaders.forEach((name, values) -> values.forEach(value -> exchange.getResponseHeaders().add(name, value)));

		sendResponse(exchange, responseCode, null, asBytes(upstreamIn), contentLength);
	}
}
