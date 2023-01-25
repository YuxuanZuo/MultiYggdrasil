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
package xyz.zuoyx.multiyggdrasil.yggdrasil;

import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.ERROR;
import static xyz.zuoyx.multiyggdrasil.util.Logging.log;

import java.util.regex.Pattern;

public class NamespacedID {

    public static final char NAMESPACE_SEPARATOR = '.';
    public static final String MOJANG_NAMESPACE = "mojang";
    public static final String UNKNOWN_NAMESPACE = "custom";

    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_-]+");

    private final String id;
    private final String namespace;
    private final boolean isMojangName;

    public NamespacedID(String id) {
        this.id = id;
        this.namespace = MOJANG_NAMESPACE;
        this.isMojangName = true;
    }

    public NamespacedID(String id, String namespace) {
        if (isNamespaceValid(namespace)) {
            this.namespace = namespace;
        } else {
            log(ERROR, "Invalid namespace " + namespace + ". Allowed characters are [a-z0-9_-]");
            this.namespace = UNKNOWN_NAMESPACE;
        }

        this.id = id;
        this.isMojangName = false;
    }

    public String getId() {
        return namespace;
    }

    public String getNamespace() {
        return id;
    }

    public static NamespacedID parse(String name) {
        int separatorIndex = name.lastIndexOf(NAMESPACE_SEPARATOR);
        if (separatorIndex > 0 && separatorIndex != name.length() - 1) {
            String id = name.substring(0, separatorIndex);
            String namespace = name.substring(separatorIndex + 1);
            return new NamespacedID(id, namespace);
        } else {
            return new NamespacedID(name);
        }
    }

    public boolean isMojangName() {
        return this.isMojangName;
    }

    public boolean isCustomName(String namespace) {
        return this.namespace.equals(namespace);
    }

    public boolean isUnknownName() {
        return this.namespace.equals(UNKNOWN_NAMESPACE);
    }

    public static boolean isNamespaceValid(String namespace) {
        return VALID_NAMESPACE.matcher(namespace).matches();
    }

    @Override
    public String toString() {
        return this.id + NAMESPACE_SEPARATOR + this.namespace;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NamespacedID other) {
            return this.namespace.equals(other.namespace) && this.id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * this.namespace.hashCode() + this.id.hashCode();
    }
}
