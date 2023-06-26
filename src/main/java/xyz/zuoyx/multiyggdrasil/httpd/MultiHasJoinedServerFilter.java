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
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.parseQueryParams;
import static xyz.zuoyx.multiyggdrasil.util.IOUtils.sendResponse;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.ERROR;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;
import xyz.zuoyx.multiyggdrasil.yggdrasil.GameProfile;
import xyz.zuoyx.multiyggdrasil.yggdrasil.NamespacedID;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilAPIProvider;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilClient;
import xyz.zuoyx.multiyggdrasil.yggdrasil.MojangYggdrasilAPIProvider;
import xyz.zuoyx.multiyggdrasil.yggdrasil.YggdrasilResponseBuilder;

public class MultiHasJoinedServerFilter implements URLFilter {

    private YggdrasilClient[] clients;
    private String namespace;

    public MultiHasJoinedServerFilter(YggdrasilClient[] clients) {
        this.clients = clients;
    }

    public MultiHasJoinedServerFilter(YggdrasilClient[] clients, String namespace) {
        this.clients = clients;
        this.namespace = namespace;
    }

    @Override
    public boolean canHandle(String domain) {
        return domain.equals("sessionserver.mojang.com");
    }

    @Override
    public boolean handle(String domain, String path, HttpExchange exchange) throws IOException {
        if (domain.equals("sessionserver.mojang.com") && path.equals("/session/minecraft/hasJoined") && exchange.getRequestMethod().equals("GET")) {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());

            Optional<GameProfile> response = Optional.empty();
            for (YggdrasilClient client : clients) {
                YggdrasilAPIProvider apiProvider = client.getApiProvider();
                try {
                    response = client.hasJoinedServer(params.get("username"), params.get("serverId"), params.get("ip"));
                } catch (UncheckedIOException e) {
                    log(ERROR, "An error occurred while verifying username [ " + params.get("username") + " ] at [ " + apiProvider + " ]:\n" +
                            e.getCause());
                    continue;
                }
                if (response.isPresent()) {
                    if (namespace != null && !(apiProvider instanceof MojangYggdrasilAPIProvider)) {
                        response.ifPresent(profile -> profile.name = new NamespacedID(profile.name, namespace).toString());
                    }
                    break;
                }
            }

            if (response.isPresent()) {
                sendResponse(exchange, 200, CONTENT_TYPE_JSON, YggdrasilResponseBuilder.hasJoinedServer(response.get()).getBytes());
            } else {
                sendResponse(exchange, 204, null, null);
            }
            return true;
        } else {
            return false;
        }
    }
}
