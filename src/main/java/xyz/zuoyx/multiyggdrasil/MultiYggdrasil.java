/*
 * Copyright (C) 2022  Haowei Wen <yushijinhun@gmail.com> and contributors
 * Copyright (C) 2022  Ethan Zuo <yuxuan.zuo@outlook.com>
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
package xyz.zuoyx.multiyggdrasil;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asBytes;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asString;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.removeNewLines;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.asBoolean;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.DEBUG;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.ERROR;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.INFO;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.WARNING;
import static xyz.zuoyx.multiyggdrasil.yggdrasil.NamespacedID.UNKNOWN_NAMESPACE;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import xyz.zuoyx.multiyggdrasil.httpd.DefaultURLRedirector;
import xyz.zuoyx.multiyggdrasil.httpd.LegacySkinAPIFilter;
import xyz.zuoyx.multiyggdrasil.httpd.ProfileKeyFilter;
import xyz.zuoyx.multiyggdrasil.httpd.AntiFeaturesFilter;
import xyz.zuoyx.multiyggdrasil.httpd.MultiHasJoinedServerFilter;
import xyz.zuoyx.multiyggdrasil.httpd.MultiQueryProfileFilter;
import xyz.zuoyx.multiyggdrasil.httpd.MultiQueryUUIDsFilter;
import xyz.zuoyx.multiyggdrasil.httpd.QueryProfileFilter;
import xyz.zuoyx.multiyggdrasil.httpd.QueryUUIDsFilter;
import xyz.zuoyx.multiyggdrasil.httpd.URLFilter;
import xyz.zuoyx.multiyggdrasil.httpd.URLProcessor;
import xyz.zuoyx.multiyggdrasil.transform.ClassTransformer;
import xyz.zuoyx.multiyggdrasil.transform.DumpClassListener;
import xyz.zuoyx.multiyggdrasil.transform.support.AuthServerNameInjector;
import xyz.zuoyx.multiyggdrasil.transform.support.AuthlibLogInterceptor;
import xyz.zuoyx.multiyggdrasil.transform.support.BungeeCordAllowedCharactersTransformer;
import xyz.zuoyx.multiyggdrasil.transform.support.CitizensTransformer;
import xyz.zuoyx.multiyggdrasil.transform.support.ConstantURLTransformUnit;
import xyz.zuoyx.multiyggdrasil.transform.support.MainArgumentsTransformer;
import xyz.zuoyx.multiyggdrasil.transform.support.PaperUsernameCheckTransformer;
import xyz.zuoyx.multiyggdrasil.transform.support.ProxyParameterWorkaround;
import xyz.zuoyx.multiyggdrasil.transform.support.SkinWhitelistTransformUnit;
import xyz.zuoyx.multiyggdrasil.transform.support.UsernameCharacterCheckTransformer;
import xyz.zuoyx.multiyggdrasil.transform.support.YggdrasilKeyTransformUnit;
import xyz.zuoyx.multiyggdrasil.transform.support.HasJoinedServerTransformer;
import xyz.zuoyx.multiyggdrasil.transform.support.HasJoinedServerResponseTransformer;
import xyz.zuoyx.multiyggdrasil.util.JsonUtils;
import xyz.zuoyx.multiyggdrasil.yggdrasil.CustomYggdrasilAPIProvider;
import xyz.zuoyx.multiyggdrasil.yggdrasil.MojangYggdrasilAPIProvider;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilClient;

public final class MultiYggdrasil {
	private MultiYggdrasil() {}

	private static boolean booted = false;
	private static Instrumentation instrumentation;
	private static boolean retransformSupported;
	private static ClassTransformer classTransformer;

	public static synchronized void bootstrap(Instrumentation instrumentation, String apiUrl) throws InitializationException {
		if (booted) {
			log(INFO, "Already started, skipping");
			return;
		}
		booted = true;
		MultiYggdrasil.instrumentation = requireNonNull(instrumentation);
		Config.init();

		retransformSupported = instrumentation.isRetransformClassesSupported();
		if (!retransformSupported) {
			log(WARNING, "Retransform is not supported");
		}

		log(INFO, "Version: " + MultiYggdrasil.class.getPackage().getImplementationVersion());

		APIMetadata apiMetadata = fetchAPIMetadata(apiUrl);
		classTransformer = createTransformer(apiMetadata);
		instrumentation.addTransformer(classTransformer, retransformSupported);

		ProxyParameterWorkaround.init();
		if (!Config.noShowServerName) {
			AuthServerNameInjector.init(apiMetadata);
		}
	}

	private static APIMetadata fetchAPIMetadata(String apiUrl) {
		if (apiUrl == null || apiUrl.isEmpty()) {
			log(ERROR, "No authentication server specified");
			throw new InitializationException();
		}

		apiUrl = addHttpsIfMissing(apiUrl);
		log(INFO, "Authentication server: " + apiUrl);
		warnIfHttp(apiUrl);

		String metadataResponse;

		Optional<String> prefetched = ofNullable(System.getProperty("authlibinjector.yggdrasil.prefetched"));
		if (prefetched.isPresent()) {

			log(DEBUG, "Prefetched metadata detected");
			try {
				metadataResponse = new String(Base64.getDecoder().decode(removeNewLines(prefetched.get())), UTF_8);
			} catch (IllegalArgumentException e) {
				log(ERROR, "Unable to decode metadata: " + e + "\n"
						+ "Encoded metadata:\n"
						+ prefetched.get());
				throw new InitializationException(e);
			}

		} else {

			try {
				HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();

				String ali = connection.getHeaderField("x-authlib-injector-api-location");
				if (ali != null) {
					URL absoluteAli = new URL(connection.getURL(), ali);
					if (!urlEqualsIgnoreSlash(apiUrl, absoluteAli.toString())) {

						// usually the URL that ALI points to is on the same host
						// so the TCP connection can be reused
						// we need to consume the response to make the connection reusable
						try (InputStream in = connection.getInputStream()) {
							while (in.read() != -1)
								;
						} catch (IOException e) {
						}

						log(INFO, "Redirect to: " + absoluteAli);
						apiUrl = absoluteAli.toString();
						warnIfHttp(apiUrl);
						connection = (HttpURLConnection) absoluteAli.openConnection();
					}
				}

				try (InputStream in = connection.getInputStream()) {
					metadataResponse = asString(asBytes(in));
				}
			} catch (IOException e) {
				log(ERROR, "Failed to fetch metadata: " + e);
				throw new InitializationException(e);
			}

		}

		log(DEBUG, "Metadata: " + metadataResponse);

		if (!apiUrl.endsWith("/")) {
			apiUrl += "/";
		}

		APIMetadata metadata;
		try {
			metadata = APIMetadata.parse(apiUrl, metadataResponse);
		} catch (UncheckedIOException e) {
			log(ERROR, "Unable to parse metadata: " + e.getCause() + "\n"
					+ "Raw metadata:\n"
					+ metadataResponse);
			throw new InitializationException(e);
		}
		log(DEBUG, "Parsed metadata: " + metadata);
		return metadata;
	}

	public static String getNamespace(APIMetadata meta) {
		return ofNullable(Config.namespace).orElseGet(
				() -> ofNullable(meta.getMeta().get("namespace"))
						.filter(element -> element.getAsJsonPrimitive().isString())
						.map(JsonUtils::asJsonString)
						.orElse(UNKNOWN_NAMESPACE)
		);
	}

	private static void warnIfHttp(String url) {
		if (url.toLowerCase().startsWith("http://")) {
			log(WARNING, "You are using HTTP protocol, which is INSECURE! Please switch to HTTPS if possible.");
		}
	}

	private static String addHttpsIfMissing(String url) {
		String lowercased = url.toLowerCase();
		if (!lowercased.startsWith("http://") && !lowercased.startsWith("https://")) {
			url = "https://" + url;
		}
		return url;
	}

	private static boolean urlEqualsIgnoreSlash(String a, String b) {
		if (!a.endsWith("/"))
			a += "/";
		if (!b.endsWith("/"))
			b += "/";
		return a.equals(b);
	}

	private static List<URLFilter> createFilters(APIMetadata config) {
		if (Config.httpdDisabled) {
			log(INFO, "Disabled local HTTP server");
			return emptyList();
		}

		List<URLFilter> filters = new ArrayList<>();

		YggdrasilClient customClient = new YggdrasilClient(new CustomYggdrasilAPIProvider(config));
		YggdrasilClient mojangClient = new YggdrasilClient(new MojangYggdrasilAPIProvider(), Config.mojangProxy);

		boolean legacySkinPolyfillDefault = !Boolean.TRUE.equals(asBoolean(config.getMeta().get("feature.legacy_skin_api")));
		if (Config.legacySkinPolyfill.isEnabled(legacySkinPolyfillDefault)) {
			filters.add(new LegacySkinAPIFilter(customClient));
		} else {
			log(INFO, "Disabled legacy skin API polyfill");
		}

		boolean mojangYggdrasilServiceDefault = Boolean.TRUE.equals(asBoolean(config.getMeta().get("feature.enable_mojang_yggdrasil_service")));
		if (Config.mojangYggdrasilService.isEnabled(mojangYggdrasilServiceDefault)) {
			log(INFO, "Mojang Yggdrasil service is enabled, Mojang namespace will be disabled!");
			String namespace = getNamespace(config);

			YggdrasilClient[] clients;
			if (Config.priorityVerifyingCustomName) {
				clients = new YggdrasilClient[]{customClient, mojangClient};
			} else {
				clients = new YggdrasilClient[]{mojangClient, customClient};
			}

			if (Config.noNamespaceSuffix) {
				filters.add(new MultiHasJoinedServerFilter(clients));
			} else {
				filters.add(new MultiHasJoinedServerFilter(clients, namespace));
			}
			filters.add(new MultiQueryUUIDsFilter(mojangClient, customClient, namespace));
			filters.add(new MultiQueryProfileFilter(mojangClient, customClient, namespace));
		} else {
			log(INFO, "Disabled Mojang Yggdrasil service");
		}

		boolean mojangNamespaceDefault = !Boolean.TRUE.equals(asBoolean(config.getMeta().get("feature.no_mojang_namespace")));
		if (Config.mojangNamespace.isEnabled(mojangNamespaceDefault) && !Config.mojangYggdrasilService.isEnabled(mojangYggdrasilServiceDefault)) {
			filters.add(new QueryUUIDsFilter(mojangClient, customClient));
			filters.add(new QueryProfileFilter(mojangClient, customClient));
		} else {
			log(INFO, "Disabled Mojang namespace");
		}

		boolean mojangAntiFeaturesDefault = Boolean.TRUE.equals(asBoolean(config.getMeta().get("feature.enable_mojang_anti_features")));
		if (!Config.mojangAntiFeatures.isEnabled(mojangAntiFeaturesDefault)) {
			filters.add(new AntiFeaturesFilter());
		}

		boolean profileKeyDefault = Boolean.TRUE.equals(asBoolean(config.getMeta().get("feature.enable_profile_key")));
		if (!Config.profileKey.isEnabled(profileKeyDefault)) {
			filters.add(new ProfileKeyFilter());
		}

		return filters;
	}

	private static ClassTransformer createTransformer(APIMetadata config) {
		URLProcessor urlProcessor = new URLProcessor(createFilters(config), new DefaultURLRedirector(config));

		ClassTransformer transformer = new ClassTransformer();
		transformer.setIgnores(Config.ignoredPackages);

		if (Config.dumpClass) {
			transformer.listeners.add(new DumpClassListener(Paths.get("").toAbsolutePath()));
		}

		if (Config.authlibLogging) {
			transformer.units.add(new AuthlibLogInterceptor());
		}

		transformer.units.add(new MainArgumentsTransformer());
		transformer.units.add(new ConstantURLTransformUnit(urlProcessor));
		transformer.units.add(new CitizensTransformer());
		transformer.units.add(new BungeeCordAllowedCharactersTransformer());
		transformer.units.add(new UsernameCharacterCheckTransformer());
		transformer.units.add(new PaperUsernameCheckTransformer());
		transformer.units.add(new HasJoinedServerTransformer());
		transformer.units.add(new HasJoinedServerResponseTransformer());

		transformer.units.add(new SkinWhitelistTransformUnit());
		SkinWhitelistTransformUnit.getWhitelistedDomains().addAll(config.getSkinDomains());

		transformer.units.add(new YggdrasilKeyTransformUnit());
		config.getDecodedPublickey().ifPresent(YggdrasilKeyTransformUnit.PUBLIC_KEYS::add);

		return transformer;
	}

	public static void retransformClasses(String... classNames) {
		if (!retransformSupported) {
			return;
		}
		Set<String> classNamesSet = new HashSet<>(Arrays.asList(classNames));
		Class<?>[] classes = Stream.of(instrumentation.getAllLoadedClasses())
				.filter(clazz -> classNamesSet.contains(clazz.getName()))
				.filter(MultiYggdrasil::canRetransformClass)
				.toArray(Class[]::new);
		if (classes.length > 0) {
			log(INFO, "Attempt to retransform classes: " + Arrays.toString(classes));
			try {
				instrumentation.retransformClasses(classes);
			} catch (Throwable e) {
				log(WARNING, "Failed to retransform", e);
			}
		}
	}

	public static void retransformAllClasses() {
		if (!retransformSupported) {
			return;
		}
		log(INFO, "Attempt to retransform all classes");
		long t0 = System.currentTimeMillis();

		Class<?>[] classes = Stream.of(instrumentation.getAllLoadedClasses())
				.filter(MultiYggdrasil::canRetransformClass)
				.toArray(Class[]::new);
		if (classes.length > 0) {
			try {
				instrumentation.retransformClasses(classes);
			} catch (Throwable e) {
				log(WARNING, "Failed to retransform", e);
				return;
			}
		}

		long t1 = System.currentTimeMillis();
		log(INFO, "Retransformed " + classes.length + " classes in " + (t1 - t0) + "ms");
	}

	private static boolean canRetransformClass(Class<?> clazz) {
		if (!instrumentation.isModifiableClass(clazz)) {
			return false;
		}
		String name = clazz.getName();
		for (String prefix : Config.ignoredPackages) {
			if (name.startsWith(prefix)) {
				return false;
			}
		}
		return true;
	}

	public static ClassTransformer getClassTransformer() {
		return classTransformer;
	}
}
