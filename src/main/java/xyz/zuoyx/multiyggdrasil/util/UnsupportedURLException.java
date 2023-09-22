/*
 * Copyright (C) 2023  Ethan Zuo <yuxuan.zuo@outlook.com>
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
package xyz.zuoyx.multiyggdrasil.util;

import java.io.Serial;

public class UnsupportedURLException extends Exception {
    @Serial
    private static final long serialVersionUID = 7895188952767140345L;

    public UnsupportedURLException() {
        this(null, null);
    }

    public UnsupportedURLException(String message) {
        this(message, null);
    }

    public UnsupportedURLException(String message, Throwable cause) {
        super(message, cause, false, false);
    }

    public UnsupportedURLException(Throwable cause) {
        this(null, cause);
    }
}
