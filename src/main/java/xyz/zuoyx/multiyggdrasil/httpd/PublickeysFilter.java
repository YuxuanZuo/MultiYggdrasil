/*
 * Copyright (C) 2023  Haowei Wen <yushijinhun@gmail.com> and contributors
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
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.toJsonString;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.IHTTPSession;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Response;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Status;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import xyz.zuoyx.multiyggdrasil.transform.support.YggdrasilKeyTransformUnit;

public class PublickeysFilter implements URLFilter {

	@Override
	public boolean canHandle(String domain) {
		return domain.equals("api.minecraftservices.com");
	}

	@Override
	public Optional<Response> handle(String domain, String path, IHTTPSession session) throws IOException {
		if (domain.equals("api.minecraftservices.com") && path.equals("/publickeys") && session.getMethod().equals("GET")) {
			return Optional.of(Response.newFixedLength(Status.OK, CONTENT_TYPE_JSON, toJsonString(makePublickeysResponse())));
		}
		return Optional.empty();
	}

	private JsonObject makePublickeysResponse() {
		JsonObject response = new JsonObject();
		JsonArray profilePropertyKeys = new JsonArray();
		JsonArray playerCertificateKeys = new JsonArray();

		for (PublicKey key : YggdrasilKeyTransformUnit.PUBLIC_KEYS) {
			JsonObject entry = new JsonObject();
			entry.addProperty("publicKey", Base64.getEncoder().encodeToString(key.getEncoded()));
			profilePropertyKeys.add(entry);
			playerCertificateKeys.add(entry);
		}

		response.add("profilePropertyKeys", profilePropertyKeys);
		response.add("playerCertificateKeys", playerCertificateKeys);
		return response;
	}
}
