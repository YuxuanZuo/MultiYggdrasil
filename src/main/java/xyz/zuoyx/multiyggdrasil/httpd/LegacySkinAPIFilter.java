/*
 * Copyright (C) 2020  Haowei Wen <yushijinhun@gmail.com> and contributors
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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Optional.ofNullable;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_IMAGE;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asString;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.http;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.newUncheckedIOException;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.sendResponse;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.parseJson;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.DEBUG;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.INFO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import xyz.zuoyx.multiyggdrasil.util.JsonUtils;
import xyz.zuoyx.multiyggdrasil.util.UnsupportedURLException;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilClient;

public class LegacySkinAPIFilter implements URLFilter {

	private static final Pattern PATH_SKINS = Pattern.compile("^/MinecraftSkins/(?<username>[^/]+)\\.png$");

	private YggdrasilClient upstream;

	public LegacySkinAPIFilter(YggdrasilClient upstream) {
		this.upstream = upstream;
	}

	@Override
	public boolean canHandle(String domain) {
		return domain.equals("skins.minecraft.net");
	}

	@Override
	public void handle(String domain, String path, HttpExchange exchange) throws UnsupportedURLException, IOException {
		if (!domain.equals("skins.minecraft.net"))
			throw new UnsupportedURLException();
		Matcher matcher = PATH_SKINS.matcher(path);
		if (!matcher.find())
			throw new UnsupportedURLException();
		String username = matcher.group("username");

		// Minecraft does not encode non-ASCII characters in URLs
		// We have to workaround this problem
		username = correctEncoding(username);

		Optional<String> skinUrl;
		try {
			skinUrl = upstream.queryUUID(username)
					.flatMap(uuid -> upstream.queryProfile(uuid, false))
					.flatMap(profile -> Optional.ofNullable(profile.properties.get("textures")))
					.map(property -> asString(Base64.getDecoder().decode(property.value)))
					.flatMap(texturesPayload -> obtainTextureUrl(texturesPayload, "SKIN"));
		} catch (UncheckedIOException e) {
			throw newUncheckedIOException("Failed to fetch skin metadata for " + username, e);
		}

		if (skinUrl.isPresent()) {
			String url = skinUrl.get();
			log(DEBUG, "Retrieving skin for " + username + " from " + url);
			byte[] data;
			try {
				data = http("GET", url);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Invalid URL [" + url + "]");
			} catch (IOException e) {
				throw newUncheckedIOException("Failed to retrieve skin from " + url, e);
			}
			log(INFO, "Retrieved skin for " + username + " from " + url + ", " + data.length + " bytes");
			sendResponse(exchange, 200, CONTENT_TYPE_IMAGE, data);

		} else {
			log(INFO, "No skin is found for " + username);
			sendResponse(exchange, 404, null, null);
		}
	}

	private Optional<String> obtainTextureUrl(String texturesPayload, String textureType) throws UncheckedIOException {
		JsonObject payload = parseJson(texturesPayload).getAsJsonObject();
		JsonObject textures = payload.get("textures").getAsJsonObject();

		return ofNullable(textures.get(textureType))
				.map(JsonElement::getAsJsonObject)
				.map(it -> ofNullable(it.get("url"))
						.map(JsonUtils::asJsonString)
						.orElseThrow(() -> newUncheckedIOException("Invalid JSON: Missing texture url")));
	}

	private static String correctEncoding(String grable) {
		// platform charset is used
		return new String(grable.getBytes(ISO_8859_1));
	}
}
