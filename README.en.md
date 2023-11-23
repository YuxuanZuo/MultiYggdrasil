 * **English**
 * [简体中文(Chinese Simplified)](https://github.com/YuxuanZuo/MultiYggdrasil/blob/develop/README.md)

# MultiYggdrasil
[![latest release](https://img.shields.io/github/v/tag/YuxuanZuo/MultiYggdrasil?color=yellow&include_prereleases&label=version&sort=semver&style=flat-square)](https://github.com/YuxuanZuo/MultiYggdrasil/releases)
[![ci status](https://img.shields.io/github/actions/workflow/status/YuxuanZuo/MultiYggdrasil/ci.yml?branch=develop)](https://github.com/YuxuanZuo/MultiYggdrasil/actions?query=workflow%3ACI)
[![license agpl-3.0](https://img.shields.io/badge/license-AGPL--3.0-blue.svg?style=flat-square)](https://github.com/YuxuanZuo/MultiYggdrasil/blob/develop/LICENSE)

A fork of authlib-injector with support for coexist with the Mojang authentication server.

**For more information about authlib-injector, [see the wiki](https://github.com/yushijinhun/authlib-injector/wiki) for documents and detailed descriptions.**

## Download
You can download the latest MultiYggdrasil build from [here](https://MultiYggdrasil.zuoyx.xyz/).

## Build
Dependencies: Gradle, JDK 17+

Run:
```
gradle
```
Build output can be found in `build/libs`.

## Deploy
Configure Minecraft server with the following JVM parameter:
```
-javaagent:{/path/to/MultiYggdrasil.jar}={Authentication Server URL}
```
Note: Unless the custom authentication server supports Mojang authentication server coexistence, this feature will not 
be enabled by default. You need to enable it by adding specific JVM parameters. Please refer to the [Options](README.en.md#options) 
section for more details.

## Options
```
-Dauthlibinjector.mojangNamespace={default|enabled|disabled}
    Whether to enable Mojang namespace (@mojang suffix).
    It's enabled by default if the authentication server does NOT send feature.no_mojang_namespace option.

    If enabled, virtual player <username>@mojang will have the same skin as premium (Mojang) player <username>.
    For example,
     - /give @p minecraft:skull 1 3 {SkullOwner:"Notch@mojang"}
     - /npc skin Notch@mojang
    will display Notch's skin.

    Note that the virtual player does NOT have the same UUID as its corresponding premium player.
    To distinguish virtual players from actual ones, the most significant bit of time_hi_and_version is set to 1 (see RFC 4122 section 4.1.3).
    For example:
      069a79f4-44e9-4726-a5be-fca90e38aaf5 Notch
      069a79f4-44e9-c726-a5be-fca90e38aaf5 Notch@mojang
    We use this approach because, in RFC 4122, UUID version has only 6 possible values (0~5), which means the most significant is always 0.
    In fact, Mojang uses version-4 (random) UUID, so its corresponding virtual player has a version-12 UUID.

-Dauthlibinjector.mojangProxy={proxy server URL}
    Use proxy when accessing Mojang authentication service.
    Only SOCKS protocol is supported.
    URL format: socks://<host>:<port>

    This proxy setting only affects Mojang namespace and Mojang Yggdrasil server feature, and the proxy is used only when accessing Mojang's servers.
    To enable proxy for your customized authentication server, see https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html .

-Dauthlibinjector.legacySkinPolyfill={default|enabled|disabled}
    Whether to polyfill legacy skin API, namely 'GET /skins/MinecraftSkins/{username}.png'.
    It's enabled by default if the authentication server does NOT send feature.legacy_skin_api option.

-Dauthlibinjector.debug (equals -Dauthlibinjector.debug=verbose,authlib)
 or -Dauthlibinjector.debug={comma-separated debug options}
    Available debug options:
     - verbose             enable verbose logging
     - authlib             print logs from Mojang authlib
     - dumpClass           dump modified classes
     - printUntransformed  print classes that are analyzed but not transformed, implies 'verbose'

-Dauthlibinjector.ignoredPackages={comma-separated package list}
    Ignore specified packages. Classes in these packages will not be analyzed or modified.

-Dauthlibinjector.disableHttpd
    Disable local HTTP server.
    Features (see below) depending on local HTTP server will be unavailable:
     - Mojang Yggdrasil server
     - Mojang namespace
     - Legacy skin API polyfill

-Dauthlibinjector.httpdPort={port}
    Sets the port used by the local HTTP server, defaults to 0 (randomly chosen).

-Dauthlibinjector.noShowServerName
    Do not show authentication server name in Minecraft menu screen.
    By default, MultiYggdrasil alters --versionType parameter to display the authentication server name.
    This feature can be disabled using this option.

-Dauthlibinjector.mojangAntiFeatures={default|enabled|disabled}
    Whether to turn on Minecraft's anti-features.
    It's disabled by default if the authentication server does NOT send feature.enable_mojang_anti_features option.

    These anti-features include:
     - Minecraft server blocklist
     - The API to query user privileges:
       * Online chat (allowed if the option is disabled)
       * Multiplayer (allowed if the option is disabled)
       * Realms (allowed if the option is disabled)
       * Telemetry (turned off if the option is disabled)
       * Profanity filter (turned off if the option is disabled)

-Dauthlibinjector.profileKey={default|enabled|disabled}
    Whether to enable the profile signing key feature. This feature is introduced in 22w17a, and is used to implement the multiplayer secure chat signing.
    If this this feature is enabled, Minecraft will send a POST request to /minecraftservices/player/certificates to retrieve the key pair issued by the authentication server.
    It's disabled by default if the authentication server does NOT send feature.enable_profile_key option.

-Dauthlibinjector.usernameCheck={default|enabled|disabled}
    Whether to enable username validation. If disabled, Minecraft, BungeeCord and Paper will NOT perform username validation.
    It's disabled by default if the authentication server does NOT send feature.usernameCheck option.
    Turning on this option will prevent players whose username contains special characters from joining the server.

-Dmultiyggdrasil.mojangYggdrasilService={default|enabled|disabled}
    Whether to enable ability to coexist with the Mojang authentication server.
    It's disabled by default if the authentication server does NOT send feature.enable_mojang_yggdrasil_service option.
    
    If this option is enabled, the game server will allow players from the custom authentication server and genuine
    players to login simultaneously. This would required the custom authentication server to generate a UUID for the
    player using the version 3 (MD5 hashing) UUID generator algorithm so that it could distinguish the custom player
    from the genuine player. Note: If you used a different UUID generator algorithm than version 4 before, you MUST
    migrate your server data of the player to the new ones, otherwise you may run into some random issues.
    
    In order to distinguish the username of players from the custom authentication server from that of the Mojang server,
    the player who from the custom authentication server will add a namespace suffix to their username.
    For example:
      Notch.cust
    If the option "-Dmultiyggdrasil.namespace" is not set and the field "namespace" is not sent by authentication
    server, the server will issue a default namespace called "cust". If any fields were sent, the server will use the
    namespace that you defined earlier.
    
    Some features that conflict with Mojang Yggdrasil server will no longer available anymore:
     - Mojang namespace

-Dmultiyggdrasil.priorityVerifyingCustomName
    Make the custom authentication server a priority to verify the player when logging into the game server
    (The default is to give priority to verification of the genuine player).

-Dmultiyggdrasil.namespace={namespace string}
    Set the namespace used by the feature "Mojang authentication server". Allowed characters are a-z0-9_- .

-Dmultiyggdrasil.noNamespaceSuffix
    Do not add namespace suffix to the username.
    By default, MultiYggdrasil will automatically add namespace suffix to the username to allow players from different
    authentication servers to play simultaneously.
    This feature can be disabled using this option.
```

## License
This work is licensed under the [GNU Affero General Public License v3.0](https://github.com/YuxuanZuo/MultiYggdrasil/blob/develop/LICENSE) or later, with the "MULTIYGGDRASIL" exception.

> **"MULTIYGGDRASIL" EXCEPTION TO THE AGPL**
>
> As a special exception, using this work in the following ways does not cause your program to be covered by the AGPL:
> 1. Bundling the unaltered binary form of this work in your program without statically or dynamically linking to it; or
> 2. Interacting with this work through the provided inter-process communication interface, such as the HTTP API; or
> 3. Loading this work as a Java Agent into a Java Virtual Machine.

## Credits
* [authlib-injector](https://github.com/yushijinhun/authlib-injector) by [Haowei Wen](https://github.com/yushijinhun)  
This is the base of this project, which makes our ideas possible.
* [Gson](https://github.com/google/gson) by Google Inc.
* [ASM](https://asm.ow2.io) by INRIA, France Telecom