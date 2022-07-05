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
package xyz.zuoyx.multiyggdrasil.yggdrasil;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_JSON;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asString;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.http;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.newUncheckedIOException;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.asJsonString;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.parseJson;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.DEBUG;
import static xyz.zuoyx.multiyggdrasil.util.UUIDUtils.fromUnsignedUUID;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.zuoyx.multiyggdrasil.util.JsonUtils;
import xyz.zuoyx.multiyggdrasil.yggdrasil.GameProfile.PropertyValue;

public class YggdrasilClient {

	private YggdrasilAPIProvider apiProvider;
	private Proxy proxy;

	public YggdrasilClient(YggdrasilAPIProvider apiProvider) {
		this(apiProvider, null);
	}

	public YggdrasilClient(YggdrasilAPIProvider apiProvider, Proxy proxy) {
		this.apiProvider = apiProvider;
		this.proxy = proxy;
	}

	public Map<String, UUID> queryUUIDs(Set<String> names) throws UncheckedIOException {
		String responseText;
		try {
			responseText = asString(http("POST", apiProvider.queryUUIDsByNames(),
					JsonUtils.toJsonString(names).getBytes(UTF_8), CONTENT_TYPE_JSON,
					proxy));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log(DEBUG, "Query UUIDs of " + names + " at [" + apiProvider + "], response: " + responseText);

		Map<String, UUID> result = new LinkedHashMap<>();
		for (JsonElement rawProfile : parseJson(responseText).getAsJsonArray()) {
			JsonObject profile = rawProfile.getAsJsonObject();
			result.put(
					asJsonString(profile.get("name")),
					parseUnsignedUUID(asJsonString(profile.get("id"))));
		}
		return result;
	}

	public Optional<UUID> queryUUID(String name) throws UncheckedIOException {
		return Optional.ofNullable(queryUUIDs(singleton(name)).get(name));
	}

	public Optional<GameProfile> queryProfile(UUID uuid, boolean withSignature) throws UncheckedIOException {
		String url = apiProvider.queryProfile(uuid);
		if (withSignature) {
			url += "?unsigned=false";
		}
		String responseText;
		try {
			responseText = asString(http("GET", url, proxy));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (responseText.isEmpty()) {
			log(DEBUG, "Query profile of [" + uuid + "] at [" + apiProvider + "], not found");
			return Optional.empty();
		}
		log(DEBUG, "Query profile of [" + uuid + "] at [" + apiProvider + "], response: " + responseText);

		return Optional.of(parseGameProfile(parseJson(responseText).getAsJsonObject()));
	}

	public Optional<GameProfile> hasJoinedServer(String username, String serverId, String ip) throws UncheckedIOException {
		String responseText;
		try {
			responseText = asString(http("GET", apiProvider.hasJoinedServer(username, serverId, ip) , proxy));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (responseText.isEmpty()) {
			log(DEBUG, "Username [" + username + "] is not authenticated at [" + apiProvider + "] Yggdrasil server");
			return Optional.empty();
		}
		log(DEBUG, "Username [" + username + "] has been authenticated at [" + apiProvider + "] Yggdrasil server, response: " + responseText);

		return Optional.of(parseGameProfile(parseJson(responseText).getAsJsonObject()));
	}

	public YggdrasilAPIProvider getApiProvider() {
		return apiProvider;
	}

	private GameProfile parseGameProfile(JsonObject json) {
		GameProfile profile = new GameProfile();
		profile.id = parseUnsignedUUID(asJsonString(json.get("id")));
		profile.name = asJsonString(json.get("name"));
		profile.properties = new LinkedHashMap<>();
		for (JsonElement rawProperty : json.get("properties").getAsJsonArray()) {
			JsonObject property = rawProperty.getAsJsonObject();
			PropertyValue entry = new PropertyValue();
			entry.value = asJsonString(property.get("value"));
			if (property.has("signature")) {
				entry.signature = asJsonString(property.get("signature"));
			}
			profile.properties.put(asJsonString(property.get("name")), entry);
		}
		return profile;
	}

	private UUID parseUnsignedUUID(String uuid) throws UncheckedIOException {
		try {
			return fromUnsignedUUID(uuid);
		} catch (IllegalArgumentException e) {
			throw newUncheckedIOException(e.getMessage());
		}
	}
}
