/*
 * Copyright (C) 2019  Haowei Wen <yushijinhun@gmail.com> and contributors
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

import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.toJsonString;
import static xyz.zuoyx.multiyggdrasil.util.UUIDUtils.toUnsignedUUID;

import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class YggdrasilResponseBuilder {
	private YggdrasilResponseBuilder() {
	}

	public static String queryUUIDs(Map<String, UUID> result) {
		JsonArray response = new JsonArray();
		result.forEach((name, uuid) -> {
			JsonObject entry = new JsonObject();
			entry.addProperty("id", toUnsignedUUID(uuid));
			entry.addProperty("name", name);
			response.add(entry);
		});
		return toJsonString(response);
	}

	public static String queryProfile(GameProfile profile, boolean withSignature) {
		JsonObject response = new JsonObject();
		response.addProperty("id", toUnsignedUUID(profile.id));
		response.addProperty("name", profile.name);

		JsonArray properties = new JsonArray();
		profile.properties.forEach((name, value) -> {
			JsonObject entry = new JsonObject();
			entry.addProperty("name", name);
			entry.addProperty("value", value.value);
			if (withSignature && value.signature != null) {
				entry.addProperty("signature", value.signature);
			}
			properties.add(entry);
		});
		response.add("properties", properties);

		return toJsonString(response);
	}

	public static String hasJoinedServer(GameProfile profile) {
		JsonObject response = new JsonObject();
		response.addProperty("id", toUnsignedUUID(profile.id));
		response.addProperty("name", profile.name);

		JsonArray properties = new JsonArray();
		profile.properties.forEach((name, value) -> {
			JsonObject entry = new JsonObject();
			entry.addProperty("name", name);
			entry.addProperty("value", value.value);
			if (value.signature != null) {
				entry.addProperty("signature", value.signature);
			}
			properties.add(entry);
		});
		response.add("properties", properties);

		return toJsonString(response);
	}
}
