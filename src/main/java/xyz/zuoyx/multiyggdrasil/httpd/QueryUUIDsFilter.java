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

import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_JSON;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asBytes;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.asString;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.asJsonString;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.parseJson;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.WARNING;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.IHTTPSession;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Response;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Status;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilClient;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilResponseBuilder;

public class QueryUUIDsFilter implements URLFilter {

	private YggdrasilClient mojangClient;
	private YggdrasilClient customClient;

	public QueryUUIDsFilter(YggdrasilClient mojangClient, YggdrasilClient customClient) {
		this.mojangClient = mojangClient;
		this.customClient = customClient;
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
			if (name.endsWith(NAME_SUFFIX)) {
				mojangNames.add(name.substring(0, name.length() - NAME_SUFFIX.length()));
			} else {
				customNames.add(name);
			}
		});

		Map<String, UUID> result = new LinkedHashMap<>();
		if (!customNames.isEmpty()) {
			result.putAll(customClient.queryUUIDs(customNames));
		}
		if (!mojangNames.isEmpty()) {
			mojangClient.queryUUIDs(mojangNames)
					.forEach((name, uuid) -> result.put(name + NAME_SUFFIX, maskUUID(uuid)));
		}
		return result;
	}

	private static final int MSB_MASK = 0x00008000;
	static final String NAME_SUFFIX = "@mojang";

	static UUID maskUUID(UUID uuid) {
		if (isMaskedUUID(uuid)) {
			log(WARNING, "UUID already masked: " + uuid);
		}
		return new UUID(uuid.getMostSignificantBits() | MSB_MASK, uuid.getLeastSignificantBits());
	}

	static boolean isMaskedUUID(UUID uuid) {
		return (uuid.getMostSignificantBits() & MSB_MASK) != 0;
	}

	static UUID unmaskUUID(UUID uuid) {
		return new UUID(uuid.getMostSignificantBits() & (~MSB_MASK), uuid.getLeastSignificantBits());
	}

}
