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

import static java.nio.charset.StandardCharsets.UTF_8;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_JSON;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.sendResponse;
import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.toJsonString;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.JsonObject;
import xyz.zuoyx.multiyggdrasil.util.UnsupportedURLException;

/**
 * Intercepts Minecraft's request to <a href="https://api.minecraftservices.com/player/certificates">...</a>,
 * and returns an empty response.
 */
public class ProfileKeyFilter implements URLFilter {

	@Override
	public boolean canHandle(String domain) {
		return domain.equals("api.minecraftservices.com");
	}

	@Override
	public void handle(String domain, String path, HttpExchange exchange) throws UnsupportedURLException, IOException {
		if (domain.equals("api.minecraftservices.com") && path.equals("/player/certificates") && exchange.getRequestMethod().equals("POST")) {
			sendResponse(exchange, 200, CONTENT_TYPE_JSON, toJsonString(makeDummyResponse()).getBytes());
		} else {
			throw new UnsupportedURLException();
		}
	}

	private JsonObject makeDummyResponse() {
		KeyPairGenerator generator;
		try {
			generator = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();

		Base64.Encoder base64 = Base64.getMimeEncoder(76, "\n".getBytes(UTF_8));
		String publicKeyPEM = "-----BEGIN RSA PUBLIC KEY-----\n" + base64.encodeToString(keyPair.getPublic().getEncoded()) + "\n-----END RSA PUBLIC KEY-----\n";
		String privateKeyPEM = "-----BEGIN RSA PRIVATE KEY-----\n" + base64.encodeToString(keyPair.getPrivate().getEncoded()) + "\n-----END RSA PRIVATE KEY-----\n";

		Instant now = Instant.now();
		Instant expiresAt = now.plus(48, ChronoUnit.HOURS);
		Instant refreshedAfter = now.plus(36, ChronoUnit.HOURS);

		JsonObject response = new JsonObject();
		JsonObject keyPairObj = new JsonObject();
		keyPairObj.addProperty("privateKey", privateKeyPEM);
		keyPairObj.addProperty("publicKey", publicKeyPEM);
		response.add("keyPair", keyPairObj);
		response.addProperty("publicKeySignature", "AA==");
		response.addProperty("publicKeySignatureV2", "AA==");
		response.addProperty("expiresAt", DateTimeFormatter.ISO_INSTANT.format(expiresAt));
		response.addProperty("refreshedAfter", DateTimeFormatter.ISO_INSTANT.format(refreshedAfter));
		return response;
	}

}
