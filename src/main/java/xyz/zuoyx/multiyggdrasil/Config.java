/*
 * Copyright (C) 2022  Haowei Wen <yushijinhun@gmail.com> and contributors
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
package xyz.zuoyx.multiyggdrasil;

import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.ERROR;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.INFO;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Config {

	/*
	 * See readme for option details.
	 */

	private Config() {}

	public static enum FeatureOption {
		DEFAULT, ENABLED, DISABLED;
		public boolean isEnabled(boolean defaultValue) {
			return this == DEFAULT ? defaultValue : this == ENABLED;
		}
	}

	public static boolean verboseLogging;
	public static boolean authlibLogging;
	public static boolean printUntransformedClass;
	public static boolean dumpClass;
	public static boolean httpdDisabled;
	public static boolean noShowServerName;
	public static boolean noLogFile;
	public static boolean noNamespaceSuffix;
	public static int httpdPort;
	public static String namespace;
	public static /* nullable */ Proxy mojangProxy;
	public static Set<String> ignoredPackages;
	public static FeatureOption mojangNamespace;
	public static FeatureOption mojangYggdrasilService;
	public static FeatureOption legacySkinPolyfill;
	public static FeatureOption mojangAntiFeatures;
	public static FeatureOption profileKey;

	private static void initDebugOptions() {
		String prop = System.getProperty("authlibinjector.debug");
		if (prop == null) {
			// all disabled if param not specified
		} else if (prop.isEmpty()) {
			verboseLogging = true;
			authlibLogging = true;
		} else {
			for (String option : prop.split(",")) {
				switch (option) {
					case "verbose" -> verboseLogging = true;
					case "authlib" -> authlibLogging = true;
					case "printUntransformed" -> {
						printUntransformedClass = true;
						verboseLogging = true;
					}
					case "dumpClass" -> dumpClass = true;
					default -> {
						log(ERROR, "Unrecognized debug option: " + option);
						throw new InitializationException();
					}
				}
			}
		}
	}

	private static final String[] DEFAULT_IGNORED_PACKAGES = {
			"xyz.zuoyx.multiyggdrasil.",
			"java.",
			"javax.",
			"jdk.",
			"com.sun.",
			"sun.",
			"net.java.",
	};

	private static void initIgnoredPackages() {
		Set<String> pkgs = new HashSet<>();
		Collections.addAll(pkgs, DEFAULT_IGNORED_PACKAGES);
		String propIgnoredPkgs = System.getProperty("authlibinjector.ignoredPackages");
		if (propIgnoredPkgs != null) {
			for (String pkg : propIgnoredPkgs.split(",")) {
				pkg = pkg.trim();
				if (!pkg.isEmpty())
					pkgs.add(pkg);
			}
		}
		ignoredPackages = Collections.unmodifiableSet(pkgs);
	}

	private static void initMojangProxy() {
		String prop = System.getProperty("authlibinjector.mojangProxy");
		if (prop == null) {
			return;
		}

		Matcher matcher = Pattern.compile("^(?<protocol>[^:]+)://(?<host>[^/]+)+:(?<port>\\d+)$").matcher(prop);
		if (!matcher.find()) {
			log(ERROR, "Unrecognized proxy URL: " + prop);
			throw new InitializationException();
		}

		String protocol = matcher.group("protocol");
		String host = matcher.group("host");
		int port = Integer.parseInt(matcher.group("port"));

		switch (protocol) {
			case "socks" -> mojangProxy = new Proxy(Type.SOCKS, new InetSocketAddress(host, port));
			default -> {
				log(ERROR, "Unsupported proxy protocol: " + protocol);
				throw new InitializationException();
			}
		}
		log(INFO, "Mojang proxy: " + mojangProxy);
	}

	private static FeatureOption parseFeatureOption(String property) {
		String prop = System.getProperty(property);
		if (prop == null) {
			return FeatureOption.DEFAULT;
		}
		try {
			return FeatureOption.valueOf(prop.toUpperCase());
		} catch (IllegalArgumentException e) {
			log(ERROR, "Invalid option: " + prop);
			throw new InitializationException(e);
		}
	}

	static void init() {
		initDebugOptions();
		initIgnoredPackages();
		initMojangProxy();

		mojangNamespace = parseFeatureOption("authlibinjector.mojangNamespace");
		mojangYggdrasilService = parseFeatureOption("multiyggdrasil.mojangYggdrasilService");
		legacySkinPolyfill = parseFeatureOption("authlibinjector.legacySkinPolyfill");
		mojangAntiFeatures = parseFeatureOption("authlibinjector.mojangAntiFeatures");
		profileKey = parseFeatureOption("authlibinjector.profileKey");
		httpdDisabled = System.getProperty("authlibinjector.disableHttpd") != null;
		noShowServerName = System.getProperty("authlibinjector.noShowServerName") != null;
		noLogFile = System.getProperty("authlibinjector.noLogFile") != null;
		noNamespaceSuffix = System.getProperty("multiyggdrasil.noNamespaceSuffix") != null;
		httpdPort = Integer.getInteger("authlibinjector.httpdPort", 0);
		namespace = System.getProperty("multiyggdrasil.namespace");
	}
}
