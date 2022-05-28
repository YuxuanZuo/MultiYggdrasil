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
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.ERROR;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import xyz.zuoyx.multiyggdrasil.Config;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.IHTTPSession;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Response;
import xyz.zuoyx.multiyggdrasil.internal.fi.iki.elonen.Status;
import xyz.zuoyx.multiyggdrasil.yggdrasil.GameProfile;
import xyz.zuoyx.multiyggdrasil.yggdrasil.NamespacedID;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilClient;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilResponseBuilder;

public class MultiHasJoinedServerFilter implements URLFilter {

    private YggdrasilClient mojangClient;
    private YggdrasilClient customClient;
    private String namespace;

    public MultiHasJoinedServerFilter(YggdrasilClient mojangClient, YggdrasilClient customClient, String namespace) {
        this.mojangClient = mojangClient;
        this.customClient = customClient;
        this.namespace = namespace;
    }

    @Override
    public boolean canHandle(String domain) {
        return domain.equals("sessionserver.mojang.com");
    }

    @Override
    public Optional<Response> handle(String domain, String path, IHTTPSession session) throws IOException {
        if (domain.equals("sessionserver.mojang.com") && path.equals("/session/minecraft/hasJoined") && session.getMethod().equals("GET")) {
            Map<String, String> params = new LinkedHashMap<>();
            session.getParameters().forEach(
                    (key ,value) -> params.put(key, value.get(0))
            );

            Optional<GameProfile> response = Optional.empty();
            try {
                response = mojangClient.hasJoinedServer(params.get("username"), params.get("serverId"), params.get("ip"));
            } catch (UncheckedIOException e) {
                log(ERROR, "An error occurred while verifying username [ " + params.get("username") + " ] at Mojang Yggdrasil server:\n" +
                        e.getCause());
            }
            if (response.isEmpty()) {
                response = customClient.hasJoinedServer(params.get("username"), params.get("serverId"), params.get("ip"));
                if (!Config.noNamespaceSuffix) {
                    response.ifPresent(profile -> profile.name = new NamespacedID(profile.name, namespace).toString());
                }
            }

            if (response.isPresent()) {
                return Optional.of(Response.newFixedLength(Status.OK, CONTENT_TYPE_JSON, YggdrasilResponseBuilder.hasJoinedServer(response.get())));
            } else {
                return Optional.of(Response.newFixedLength(Status.NO_CONTENT, null, null));
            }
        } else {
            return Optional.empty();
        }
    }
}
