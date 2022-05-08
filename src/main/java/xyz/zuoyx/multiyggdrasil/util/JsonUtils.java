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
package xyz.zuoyx.multiyggdrasil.util;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public final class JsonUtils {

	private static final Gson gson = new Gson();

	public static JsonElement parseJson(String jsonText) {
		return JsonParser.parseString(jsonText);
	}

	public static String toJsonString(Object json) {
		return gson.toJson(json);
	}

	public static String asJsonString(JsonElement json) {
		return json.getAsJsonPrimitive().getAsString();
	}

	public static List<JsonElement> toJavaList(JsonArray json) {
		Type listType = new TypeToken<List<JsonElement>>(){}.getType();
		return gson.fromJson(json, listType);
	}

	public static Map<String, JsonElement> toJavaMap(JsonObject json) {
		Type mapType = new TypeToken<Map<String, JsonElement>>(){}.getType();
		return gson.fromJson(json, mapType);
	}

	private JsonUtils() {}

}
