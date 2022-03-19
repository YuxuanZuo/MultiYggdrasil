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
package moe.yushi.authlibinjector.httpd;

import static moe.yushi.authlibinjector.util.IOUtils.CONTENT_TYPE_JSON;
import static moe.yushi.authlibinjector.util.IOUtils.asBytes;
import static moe.yushi.authlibinjector.util.IOUtils.asString;
import static moe.yushi.authlibinjector.util.JsonUtils.asJsonString;
import static moe.yushi.authlibinjector.util.JsonUtils.parseJson;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import moe.yushi.authlibinjector.internal.fi.iki.elonen.IHTTPSession;
import moe.yushi.authlibinjector.internal.fi.iki.elonen.Response;
import moe.yushi.authlibinjector.internal.fi.iki.elonen.Status;
import moe.yushi.authlibinjector.yggdrasil.NamespacedID;
import moe.yushi.authlibinjector.yggdrasil.YggdrasilClient;
import moe.yushi.authlibinjector.yggdrasil.YggdrasilResponseBuilder;

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
	public Optional<Response> handle(String domain, String path, IHTTPSession session) throws IOException {
		if (domain.equals("api.mojang.com") && path.equals("/profiles/minecraft") && session.getMethod().equals("POST")) {
			Set<String> request = new LinkedHashSet<>();
			parseJson(asString(asBytes(session.getInputStream()))).getAsJsonArray()
					.forEach(element -> request.add(asJsonString(element)));
			return Optional.of(Response.newFixedLength(Status.OK, CONTENT_TYPE_JSON,
					YggdrasilResponseBuilder.queryUUIDs(performQuery(request))));
		} else {
			return Optional.empty();
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
