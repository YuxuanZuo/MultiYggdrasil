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

import static java.util.Optional.empty;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.CONTENT_TYPE_JSON;
import static xyz.zuoyx.multiyggdrasil.util.UUIDUtils.fromUnsignedUUID;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.IHTTPSession;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Response;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Status;
import xyz.zuoyx.multiyggdrasil.yggdrasil.GameProfile;
import xyz.zuoyx.multiyggdrasil.yggdrasil.NamespacedID;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilClient;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilResponseBuilder;

public class MultiQueryProfileFilter implements URLFilter {

	private static final Pattern PATH_REGEX = Pattern.compile("^/session/minecraft/profile/(?<uuid>[0-9a-f]{32})$");

	private YggdrasilClient mojangClient;
	private YggdrasilClient customClient;
	private String namespace;

	public MultiQueryProfileFilter(YggdrasilClient mojangClient, YggdrasilClient customClient, String namespace) {
		this.mojangClient = mojangClient;
		this.customClient = customClient;
		this.namespace = namespace;
	}

	@Override
	public boolean canHandle(String domain) {
		return domain.equals("sessionserver.mojang.com");
	}

	@Override
	public Optional<Response> handle(String domain, String path, IHTTPSession session) {
		if (!domain.equals("sessionserver.mojang.com"))
			return empty();
		Matcher matcher = PATH_REGEX.matcher(path);
		if (!matcher.find())
			return empty();

		UUID uuid;
		try {
			uuid = fromUnsignedUUID(matcher.group("uuid"));
		} catch (IllegalArgumentException e) {
			return Optional.of(Response.newFixedLength(Status.NO_CONTENT, null, null));
		}

		boolean withSignature = false;
		List<String> unsignedValues = session.getParameters().get("unsigned");
		if (unsignedValues != null && unsignedValues.get(0).equals("false")) {
			withSignature = true;
		}

		Optional<GameProfile> response;
		if (uuid.version() == 4) {
			response = mojangClient.queryProfile(uuid, withSignature);
		} else {
			response = customClient.queryProfile(uuid, withSignature);
			response.ifPresent(profile -> profile.name = new NamespacedID(profile.name, namespace).toString());
		}

		if (response.isPresent()) {
			return Optional.of(Response.newFixedLength(Status.OK, CONTENT_TYPE_JSON, YggdrasilResponseBuilder.queryProfile(response.get(), withSignature)));
		} else {
			return Optional.of(Response.newFixedLength(Status.NO_CONTENT, null, null));
		}
	}

}
