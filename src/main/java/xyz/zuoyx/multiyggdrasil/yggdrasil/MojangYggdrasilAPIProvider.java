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
package xyz.zuoyx.multiyggdrasil.yggdrasil;

import static xyz.zuoyx.multiyggdrasil.util.UUIDUtils.toUnsignedUUID;

import java.util.UUID;

public class MojangYggdrasilAPIProvider implements YggdrasilAPIProvider {

	@Override
	public String queryUUIDsByNames() {
		return "https://api.mojang.com/profiles/minecraft";
	}

	@Override
	public String queryProfile(UUID uuid) {
		return "https://sessionserver.mojang.com/session/minecraft/profile/" + toUnsignedUUID(uuid);
	}

	@Override
	public String hasJoinedServer(String username, String serverId, String ip) {
		if (ip == null) {
			return "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + username + "&serverId=" + serverId;
		} else {
			return "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + username + "&serverId=" + serverId + "&ip=" + ip;
		}
	}

	@Override
	public String toString() {
		return "Mojang";
	}
}
