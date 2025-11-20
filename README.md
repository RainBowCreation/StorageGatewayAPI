# StorageGatewayAPI

A single API for simple `get`/`set` key-value storage with **fast local reads** and **write-behind** to MySQL. It can run:
- on a **backend** (Spigot/Paper/Folia; Java 8 bytecode), and/or
- on a **proxy** (Bungee **or** Velocity; Velocity requires Java 17).

The plugin always serves from local cache immediately, then **queues** persistence to MySQL in the background. Under load or pool exhaustion, your plugin still gets fast responses; the gateway drains the queue when the DB is ready.

---

## Why this exists

- **One tiny API** (`StorageGateway.open().get()/set()`) instead of every plugin opening its own pools.
- **Write-behind**: never block the main thread on DB; `set()` returns immediately after local write.
- **Per-DB modes**: `local-only`, `direct-sql`, `proxy-sql`.
- **RW-Split**: `local-only`, `direct-sql`, `proxy-sql`.
- **Multiple entry**: `standalone jar`, `Spigot, Paper plugins`, `http api`, `cp|-Dloader.path` and so much more
- **Multi-currency Vault hook** (optional) and **PlayerPoints delegate** (optional).
- **Java8+**: Compatible with almost everything.

---

## Architecture

### Backend only (no proxy)
```

YourPlugin ──> StorageGateway (Spigot)
├─ L1 Near Cache (Caffeine)
├─ Local durability (files + WAL)
└─ [optional] async MySQL flush (writer pool)

```

### Proxy-SQL (recommended for networks)
```

YourPlugin ──> Backend SGW ──PM──> Proxy SGW ──> L1 + WAL ──queue─> MySQL
^ near cache                          (Bungee or Velocity)

````
- Backend uses plugin messaging to proxy. Backend never touches MySQL in this mode.

*(Redis L2 cache/queue is planned; flags exist but are off by default.)*

---

## Modules & Java requirements

- **plugin**: Bukkit/Spigot/Paper/Folia **and** Bungee in one JAR. Compiled with JDK 17, emits Java 8 bytecode.
- **velocity**: Velocity plugin (separate JAR, Java 17+).
- **demo-eco**: example plugin showing API usage (Java 8 bytecode).

### Build
```bash
./gradlew clean buildAll
````

---

## Using the API

Add a dependency on the **API** (exposed by `core`):

```gradle
repositories {
  maven { url 'https://repo.rainbowcreation.net/' }
}
dependencies {
  compileOnly 'net.rainbowcreation:StorageGatewayAPI:1.0.0'
}
```

Then in your plugin:

```java
import net.rainbowcreation.storage.api.StorageGateway;
import net.rainbowcreation.storage.api.StorageClient;

StorageGateway gw = getServer().getServicesManager().load(StorageGateway.class);
StorageClient c = gw.open("main", "CHANGE_ME_main_secret");

// set
c.set("players", "coins:"+uuid, 250)
 .exceptionally(err -> { getLogger().warning(err.toString()); return null; });

// get
c.get("players", "coins:"+uuid, Integer.class)
 .thenAccept(opt -> {
   int coins = opt.orElse(0);
   // switch to main thread before touching Bukkit API
   getServer().getScheduler().runTask(this, () ->
       player.sendMessage("Coins: " + coins));
 });
```

Standalone access via static class
```
StorageGateway gw = SgwAPI.get();
StorageClient c = gw.open("main", "CHANGE_ME_main_secret");

// other get set same as before
```

Api access via http endpoints default port ``7070`` or via `loadbalancer` & `eureka server`

Structure ``localhost:7070/{db}/{namespace}/{key}`` or `{loadbalancerUrl:port}/{db}/{namespace}/{key}`

 method

``get`` required ``token`` which is the secret set in ``config.yml``

``post`` required ``token`` which is the secret set in ``config.yml`` and ``value``

**Semantics**

* `set()` is **fire-and-succeed** unless the local durability step fails (e.g., disk I/O error).
* `get()` returns what’s in cache; on cold miss it may read DB (direct mode) or return local/disk if budget is saturated.

---

## Complex Class, Object

### install ``StorageGatewayAPI-template``
```
dependencies {
   implementation 'net.rainbowcreation:StorageGatewayAPI-template:1.0.0'
}
```
this module provide `AData` class and `IDataManger` interface than can be easily use for interfacing complex class|data structure with sgw


## Setup

### BungeeCord (Optional)

* Drop **StorageGatewayAPI-{version}.jar** (the **plugin** artifact) into `plugins/` on Bungee.
* Configure DBs/mode in the proxy’s `plugins/StorageGatewayAPI/config.yml`.
* Ensure `proxy.pluginMessaging.enabled: true` and a channel name (e.g. `"sgw:psync"`).
* Start Bungee; you should see “proxy ready” in logs.

### Velocity (Optional)

* Drop **StorageGatewayAPI-Velocity-{version}.jar** (the **velocity** artifact) into `plugins/` on Velocity.
* Same config file path and keys as Bungee.
* Use a **namespaced** channel (e.g. `"sgw:psync"`).
* On shutdown, the gateway flushes and stops cleanly.

### Backends

* Install **StorageGatewayAPI-{version}.jar** (the **plugin** artifact).
* Configure the same DB names/secrets.
* For `proxy` mode, set `proxy.pluginMessaging.enabled: true` and the **same** channel string as set on proxy server.
* Enable proxy mode **per DB** and on proxy server as well
* Proxy mode will ignore mysql settings and use proxy's settings instead.

---

## Performance & reliability knobs

* `limits.mysqlTotalBudget`: global permits for DB reads. If zero, all cold reads will use local/disk fallback.
* `execution.coreThreads / maxThreads / queueCapacity`: controls async workers.
* MySQL writer pool: `pool.maxPoolSize / minIdle / connectionTimeoutMs`.
* Read Your Writes: guaranteed via near-cache update on `set()`; tune `mode.readYourWritesMs` for how long to prefer cache before considering DB freshness (if you implement stricter coherence later).

---
