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

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import xyz.zuoyx.multiyggdrasil.util.UnsupportedURLException;

/**
 * A URLFilter filters the URLs in the bytecode, and intercepts those it is interested in.
 */
public interface URLFilter {

	/**
	 * Returns true if the filter MAY be interested in the given domain.
	 * <p>
	 * If this method returns true, the domain will be intercepted.
	 * And when a request is sent to this domain, handle() will be invoked.
	 * If it turns out that the filter doesn't really want to intercept the URL (handle() throws UnsupportedURLException),
	 * the request will be reverse-proxied to the original URL, as if nothing has happened.
	 */
	boolean canHandle(String domain);

	void handle(String domain, String path, HttpExchange exchange) throws UnsupportedURLException, IOException;
}
