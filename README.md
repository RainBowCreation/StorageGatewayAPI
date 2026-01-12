# StorageGatewayAPI

### Reduce DB connections by 99% while providing 0ms latency responses.

We have experienced open minecraft servers scaling to infinite players, where 10+ plugins connecting to dynamic backend nodes caused DB costs to explode. With **StorageGateway (SGW)**, we reduced Database load by **99%** while using only 25% more compute power.

This is a single API for `get`/`set` key-value storage. It acts as a **middleware** between your plugins and your MySQL database.

**The Magic:**

1. **Reads:** Served instantly from **L1 RAM Cache**.
2. **Writes:** Offloaded **instantly** to an **Async Write-Ahead Log (WAL)**. Your plugin gets a "Success" response in **0ms** without waiting for the disk or database.
3. **Sync:** A background worker intelligently batches data and pushes it to MySQL using **Smart Congestion Control**.

It can run on:

* **Backend**: Spigot/Paper/Folia (Java 8 bytecode).
* **Proxy**: Bungee or Velocity (Java 17).
* **Standalone**: Any Java application.

---

## Features

* **Smart Adaptive Batching (AIMD)**:
* SGW monitors **CPU Load**, **RAM usage**, and **Queue Depth** in real-time.
* It automatically adjusts the batch size (e.g., from 100 to 3,000) to maximize throughput without freezing the database.
* **No more lag spikes:** If the DB slows down, SGW slows down gracefully instead of crashing.


* **Crash Recovery Mode**:
* On startup, SGW detects if there is data left in the WAL (from a crash or restart).
* It enters **Recovery Mode**: The API blocks momentarily while it drains the WAL at **uncapped speed** (memory-aware) to ensure your DB is 100% consistent before letting players join.


* **Async Architecture**:
* The API thread never touches the disk. All I/O happens on a separated thread pool.



---

## Architecture

### Backend only (no proxy)

```mermaid
YourPlugin ──> SGW API ──> L1 Cache (Instant Read)
                          │
                          └─> Async WAL Thread (0ms Write)
                                    │
                                    ▼
                              Smart Batcher (AIMD)
                                    │
                                    ▼
                                  MySQL

```

### Proxy-SQL (recommended for networks)

```
YourPlugin ──> Backend SGW ──(PluginMsg)──> Proxy SGW ──> L1 + WAL ──> MySQL

```

* Backend servers never touch MySQL directly. They talk to the Proxy, which handles the heavy lifting.

---

## Why this exists

* **One Tiny API**: `StorageGateway.open().get()/set()` replaces complex SQL pools in every plugin.
* **Write-Behind**: `set()` returns immediately. Your main thread is **never blocked** by database latency.
* **Resilience**: If MySQL goes offline, SGW buffers data to disk (WAL) and replays it when the DB comes back. No data loss.
* **Multi-Platform**: Spigot, Paper, Bungee, Velocity, or Standalone Java App.

## Cons

* **No Migrations**: Designed for key-value (KV) data. Moving existing complex relational data here is difficult.
* **Opaque Data**: Data is stored as JSON/Blob in MySQL, optimized for machine reading, not human editing.

---

## Usage

### 1. Add Dependency

```gradle
repositories {
  maven { url 'https://repo.rainbowcreation.net/' }
}
dependencies {
  // or implementation if you want to connect via rest api
  compileOnly 'net.rainbowcreation:StorageGatewayAPI:1.3-SNAPSHOT'
}

```

### 2. Code Example

```java
import net.rainbowcreation.storage.api.StorageGateway;
import net.rainbowcreation.storage.api.StorageClient;

// Load the provider
StorageGateway gw = SgwAPI.get(); // Or ServiceManager on Bukkit
StorageClient c = gw.open("main", "CHANGE_ME_main_secret");

// SET (Async, Non-blocking)
c.set("players", "coins:"+uuid, 250)
 .exceptionally(err -> { 
     getLogger().warning("Local Disk Full: " + err); 
     return null; 
 });

// GET (Returns Cache or fetches DB if cold)
c.get("players", "coins:"+uuid, Integer.class)
 .thenAccept(opt -> {
   int coins = opt.orElse(0);
   System.out.println("Coins: " + coins);
 });

```

### 3. HTTP API

You can access data externally via HTTP (port 7070 default):

* `GET / {db}/{namespace}/{key}?token=SECRET`
* `POST / {db}/{namespace}/{key}?token=SECRET` (Body = Value)

---

## Setup & Configuration

### Installation

1. **Drop the JAR** into your `plugins/` folder (Bukkit, Bungee, or Velocity).
2. **Configure** `config.yml`.
3. **Restart**.

### Tuning Knobs (config.yml)

* **`queue.batchSize`**:
* `> 0`: **Static Mode**. Forces a fixed batch size (e.g., 1000). Good for predictable loads.
* `<= 0`: **Smart Mode** (Recommended). Enables the AIMD algorithm.


* **`queue.maxBatch`**:
* The "Speed Limit" for Smart Mode. SGW will try to scale up to this number but will throttle back if CPU/RAM gets tight. Recommended: `3000`.


* **`execution.coreThreads`**:
* Controls the async workers. Ensure you have enough threads for the number of databases you use.



---

## Modules

* **plugin**: Combined Jar for Bukkit/Spigot/Paper/Folia & Bungee.
* **velocity**: Dedicated Velocity support.
* **template**: Helper classes (`AData`, `IDataManager`) for serializing complex objects easily.

```bash
# Build
./gradlew clean buildAll

```
