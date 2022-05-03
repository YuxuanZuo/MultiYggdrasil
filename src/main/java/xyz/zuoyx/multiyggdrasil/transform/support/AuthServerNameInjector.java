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
package xyz.zuoyx.multiyggdrasil.transform.support;

import static xyz.zuoyx.multiyggdrasil.util.JsonUtils.asJsonString;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import com.google.gson.JsonElement;
import xyz.zuoyx.multiyggdrasil.APIMetadata;
import xyz.zuoyx.multiyggdrasil.util.Logging.Level;

public final class AuthServerNameInjector {
	private AuthServerNameInjector() {}

	private static String getServerName(APIMetadata meta) {
		JsonElement serverName = meta.getMeta().get("serverName");
		if (serverName.getAsJsonPrimitive().isString()) {
			return asJsonString(serverName);
		} else {
			return meta.getApiRoot();
		}
	}

	public static void init(APIMetadata meta) {
		MainArgumentsTransformer.getArgumentsListeners().add(args -> {
			for (int i = 0; i < args.length - 1; i++) {
				if ("--versionType".equals(args[i])) {
					String serverName = getServerName(meta);
					log(Level.DEBUG, "Setting versionType to server name: " + serverName);
					args[i + 1] = serverName;
					break;
				}
			}
			return args;
		});
	}

}
