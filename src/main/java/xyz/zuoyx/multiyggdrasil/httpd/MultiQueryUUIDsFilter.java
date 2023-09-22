/*
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
package xyz.zuoyx.multiyggdrasil.httpd;

import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_JSON;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asBytes;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asString;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.sendResponse;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.asJsonString;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.parseJson;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.sun.net.httpserver.HttpExchange;
import xyz.zuoyx.multiyggdrasil.util.UnsupportedURLException;
import xyz.zuoyx.multiyggdrasil.yggdrasil.NamespacedID;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilClient;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilResponseBuilder;

public class MultiQueryUUIDsFilter implements URLFilter {

	private YggdrasilClient mojangClient;
	private YggdrasilClient customClient;
	private String namespace;

	public MultiQueryUUIDsFilter(YggdrasilClient mojangClient, YggdrasilClient customClient, String namespace) {
		this.mojangClient = mojangClient;
		this.customClient = customClient;
		this.namespace = namespace;
	}

	@Override
	public boolean canHandle(String domain) {
		return domain.equals("api.mojang.com");
	}

	@Override
	public void handle(String domain, String path, HttpExchange exchange) throws UnsupportedURLException, IOException {
		if (domain.equals("api.mojang.com") && path.equals("/profiles/minecraft") && exchange.getRequestMethod().equals("POST")) {
			Set<String> request = new LinkedHashSet<>();
			parseJson(asString(asBytes(exchange.getRequestBody()))).getAsJsonArray()
					.forEach(element -> request.add(asJsonString(element)));
			sendResponse(exchange, 200, CONTENT_TYPE_JSON, YggdrasilResponseBuilder.queryUUIDs(performQuery(request)).getBytes());
		} else {
			throw new UnsupportedURLException();
		}
	}

	private Map<String, UUID> performQuery(Set<String> names) {
		Set<String> customNames = new LinkedHashSet<>();
		Set<String> mojangNames = new LinkedHashSet<>();
		names.forEach(name -> {
			NamespacedID namespacedID = NamespacedID.parse(name);
			if (namespacedID.isMojangName()) {
				mojangNames.add(namespacedID.getId());
			} else if (namespacedID.isCustomName(namespace)) {
				customNames.add(namespacedID.getId());
			}
		});

		Map<String, UUID> result = new LinkedHashMap<>();
		if (!mojangNames.isEmpty()) {
			result.putAll(mojangClient.queryUUIDs(mojangNames));
		}
		if (!customNames.isEmpty()) {
			customClient.queryUUIDs(customNames)
					.forEach((name, uuid) -> result.put(new NamespacedID(name, namespace).toString(), uuid));
		}
		return result;
	}
}
