package net.rainbowcreation.storage.api.template;

import net.rainbowcreation.storage.api.ModelField;
import net.rainbowcreation.storage.api.SgwAPI;
import net.rainbowcreation.storage.api.StorageClient;
import net.rainbowcreation.storage.api.StorageGateway;
import net.rainbowcreation.storage.api.annotations.QLQuery;
import net.rainbowcreation.storage.api.common.GatewayHandler;
import net.rainbowcreation.storage.api.network.SimpleTcpMessenger;
import net.rainbowcreation.storage.api.proxy.ProxyMessenger;
import net.rainbowcreation.storage.api.utils.SchemaScanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.time.Duration;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class ADataManager implements IDataManager {

    protected enum ConnectionMode {
        DIRECT_LIBRARY,
        TCP_CLIENT,
        API_CLIENT
    }

    protected ConnectionMode mode;

    // -- Backend References --
    protected StorageGateway SGW;
    protected StorageClient SC;
    protected GatewayHandler tcpHandler;
    protected ProxyMessenger tcpMessenger;

    // -- Configuration --
    protected String dbName = "main";
    protected String token = "CHANGE_ME_main_secret";

    // TCP Config
    protected boolean tcpEnabled = false;
    protected String tcpHost = "127.0.0.1";
    protected int tcpPort = 7071;

    // REST API Config
    protected String apiBaseUrl = "http://localhost:7070";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    public void initialize() {
        logInfo("Initializing Connection for DB: " + dbName);

        try {
            Class.forName("net.rainbowcreation.storage.api.SgwAPI");
            if (SgwAPI.get() != null) {
                this.SGW = SgwAPI.get();
                // Open storage client via Core
                this.SC = SGW.open(dbName, token);
                this.mode = ConnectionMode.DIRECT_LIBRARY;
                logInfo("Mode: DIRECT_LIBRARY");
                registerClasses();
                return;
            }
        } catch (Throwable ignored) {
            // Core not found
        }

        if (tcpEnabled) {
            try {
                logInfo("Connecting via TCP to " + tcpHost + ":" + tcpPort + "...");
                this.tcpMessenger = new SimpleTcpMessenger(tcpHost, tcpPort);
                this.tcpMessenger.register();
                this.tcpHandler = createSimpleHandler(dbName, token, this.tcpMessenger);
                this.mode = ConnectionMode.TCP_CLIENT;
                logInfo("Mode: TCP_CLIENT");
                registerClasses();
                return;
            } catch (Exception e) {
                logWarn("TCP Init Failed: " + e.getMessage());
            }
        }

        if (apiBaseUrl != null && !apiBaseUrl.isEmpty()) {
            this.mode = ConnectionMode.API_CLIENT;
            logInfo("Mode: API_CLIENT (" + apiBaseUrl + ")");
            return;
        }

        throw new IllegalStateException("Critical Error: No valid connection available!");
    }

    public void shutdown() {
        if (tcpMessenger != null) tcpMessenger.unregister();
        if (tcpHandler != null) tcpHandler.shutdown();
    }

    // Override for complex namespace keying (e.g., adding region prefix)
    public String nss(String ns) {
        return ns;
    }

    public void registerClasses() {
        // Implementation overrides this to call registerClass(MyModel.class)
    }

    public void registerClass(Class<?> cls) {
        if (!cls.isAnnotationPresent(QLQuery.class)) {
            logWarn("Cannot register " + cls.getSimpleName() + ": Missing @QLQuery annotation.");
            return;
        }

        QLQuery ql = cls.getAnnotation(QLQuery.class);
        String finalNs = nss(ql.namespace());
        String typeName = ql.typeName();

        try {
            if (mode == ConnectionMode.DIRECT_LIBRARY) {
                SC.registerClass(finalNs, cls);
                logInfo("Registered Direct Model: " + typeName);

            } else if (mode == ConnectionMode.TCP_CLIENT) {
                Map<String, ModelField> fields = SchemaScanner.scan(cls);
                tcpMessenger.sendRegisterModel(dbName, token, finalNs, typeName, fields);
                logInfo("Sent TCP Registration for: " + typeName);
            }
        } catch (Exception e) {
            logSevere("Failed to register class " + cls.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public <T> T get(String ns, String key, Class<T> type) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY:
                    return SC.getBlocking(n, key, type, Duration.ofMillis(500));

                case TCP_CLIENT:
                    Optional<String> opt = tcpHandler.get(n, key).join();
                    return opt.map(s -> convert(s, type)).orElse(null);

                case API_CLIENT:
                    String url = String.format("%s/%s/%s/%s?token=%s", apiBaseUrl, dbName, n, key, token);
                    return httpGet(url, type);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public <T> CompletableFuture<T> getAsync(String ns, String key, Class<T> type) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY:
                    return SC.get(n, key, type).thenApply(opt -> opt.orElse(null));

                case TCP_CLIENT:
                    return tcpHandler.get(n, key).thenApply(opt ->
                            opt.map(s -> convert(s, type)).orElse(null)
                    );

                case API_CLIENT:
                    return CompletableFuture.supplyAsync(() -> {
                        String url = String.format("%s/%s/%s/%s?token=%s", apiBaseUrl, dbName, n, key, token);
                        try { return httpGet(url, type); } catch (Exception e) { return null; }
                    });
            }
        } catch (Exception e) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void set(String ns, String key, Object value) {
        setAsync(ns, key, value); // set already in Async or if you want this to wait until set complete simply override and add .join()
    }

    @Override
    public CompletableFuture<Void> setAsync(String ns, String key, Object value) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY:
                    return SC.set(n, key, value);

                case TCP_CLIENT:
                    String json = objectMapper.writeValueAsString(value);
                    return tcpHandler.set(n, key, json);

                case API_CLIENT:
                    return CompletableFuture.runAsync(() -> {
                        try {
                            String url = String.format("%s/%s/%s/%s?token=%s", apiBaseUrl, dbName, n, key, token);
                            httpPost(url, objectMapper.writeValueAsString(value));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            }
        } catch (Exception e) {
            return failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void delete(String ns, String key) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY: SC.delete(n, key); break;
                case TCP_CLIENT:     tcpHandler.delete(n, key); break;
                case API_CLIENT:
                    String url = String.format("%s/%s/%s/%s?token=%s", apiBaseUrl, dbName, n, key, token);
                    httpDelete(url);
                    break;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public <T> List<T> get(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset, Class<T> type) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY:
                    // Use StorageClient's blocking helper
                    return SC.getBlocking(n, filters, selections, limit, offset, type, Duration.ofMillis(1000));

                case TCP_CLIENT:
                    Optional<String> res = tcpHandler.get(n, filters, selections, limit, offset).join();
                    if (res.isPresent()) {
                        return objectMapper.readValue(res.get(), objectMapper.getTypeFactory().constructCollectionType(List.class, type));
                    }
                    return new ArrayList<>();

                case API_CLIENT:
                    StringBuilder sb = new StringBuilder(String.format("%s/%s/%s?token=%s", apiBaseUrl, dbName, n, token));
                    appendQuery(sb, filters, selections, limit, offset);
                    String json = httpGetString(sb.toString());
                    if (json == null) return new ArrayList<>();
                    return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
            }
        } catch (Exception e) { return new ArrayList<>(); }
        return new ArrayList<>();
    }

    @Override
    public <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset, Class<T> type) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY:
                    return SC.get(n, filters, selections, limit, offset, type)
                            .thenApply(opt -> opt.orElse(new ArrayList<>()));

                case TCP_CLIENT:
                    return tcpHandler.get(n, filters, selections, limit, offset)
                            .thenApply(opt -> opt.map(json -> convertList(json, type)).orElse(new ArrayList<>()));

                case API_CLIENT:
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            StringBuilder sb = new StringBuilder(String.format("%s/%s/%s?token=%s", apiBaseUrl, dbName, n, token));
                            appendQuery(sb, filters, selections, limit, offset);
                            String json = httpGetString(sb.toString());
                            if (json == null) return new ArrayList<>();
                            return convertList(json, type);
                        } catch (Exception e) { return new ArrayList<>(); }
                    });
            }
        } catch (Exception e) {
            return failedFuture(e);
        }
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    public <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, Class<T> type) {
        return getAsync(ns, filters, null, 1000, 0, type);
    }

    @Override
    public Integer count(String ns, Map<String, String> filters, int limit, int offset) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY:
                    return SC.countBlocking(n, filters, limit, offset, Duration.ofMillis(1000));

                case TCP_CLIENT:
                    return tcpHandler.count(n, filters, limit, offset).join().orElse(0);

                case API_CLIENT:
                    StringBuilder sb = new StringBuilder(String.format("%s/%s/%s?token=%s&count=true", apiBaseUrl, dbName, n, token));
                    appendQuery(sb, filters, null, limit, offset);
                    String resp = httpGetString(sb.toString());
                    return (resp != null) ? Integer.parseInt(resp) : 0;
            }
        } catch (Exception e) { return 0; }
        return 0;
    }

    @Override
    public CompletableFuture<Integer> countAsync(String ns, Map<String, String> filters, int limit, int offset) {
        String n = nss(ns);
        try {
            switch (mode) {
                case DIRECT_LIBRARY:
                    return SC.count(n, filters, limit, offset)
                            .thenApply(opt -> opt.orElse(0));

                case TCP_CLIENT:
                    return tcpHandler.count(n, filters, limit, offset)
                            .thenApply(opt -> opt.orElse(0));

                case API_CLIENT:
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            StringBuilder sb = new StringBuilder(String.format("%s/%s/%s?token=%s&count=true", apiBaseUrl, dbName, n, token));
                            appendQuery(sb, filters, null, limit, offset);
                            String resp = httpGetString(sb.toString());
                            return (resp != null) ? Integer.parseInt(resp) : 0;
                        } catch (Exception e) { return 0; }
                    });
            }
        } catch (Exception e) {
            return failedFuture(e);
        }
        return CompletableFuture.completedFuture(0);
    }

    private <T> T httpGet(String urlStr, Class<T> type) throws Exception {
        String body = httpGetString(urlStr);
        return (body != null) ? convert(body, type) : null;
    }

    private String httpGetString(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 404) return null;
            if (conn.getResponseCode() >= 400) return null;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                return response.toString();
            }
        } finally { if (conn != null) conn.disconnect(); }
    }

    private void httpPost(String urlStr, String json) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            conn.getResponseCode();
        } finally { if (conn != null) conn.disconnect(); }
    }

    private void httpDelete(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setConnectTimeout(5000);
            conn.getResponseCode();
        } finally { if (conn != null) conn.disconnect(); }
    }

    private void appendQuery(StringBuilder sb, Map<String, String> filters, Map<String, String> selections, int limit, int offset) {
        sb.append("&limit=").append(limit).append("&offset=").append(offset);
        if (filters != null) filters.forEach((k, v) -> {
            try { sb.append("&").append(URLEncoder.encode(k, "UTF-8")).append("=").append(URLEncoder.encode(v, "UTF-8")); } catch (Exception ignored) {}
        });
        if (selections != null && !selections.isEmpty()) sb.append("&fields=").append(String.join(",", selections.keySet()));
    }

    private <T> T convert(String json, Class<T> type) {
        try { return objectMapper.readValue(json, type); }
        catch (Exception e) { return null; }
    }

    private <T> List<T> convertList(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private <T> CompletableFuture<T> failedFuture(Throwable ex) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(ex);
        return f;
    }

    private GatewayHandler createSimpleHandler(final String db, final String secret, final ProxyMessenger msgr) {
        return new GatewayHandler() {
            public CompletableFuture<Optional<String>> get(String ns, String key) { return msgr.get(db, secret, ns, key); }
            public CompletableFuture<Optional<String>> get(String ns, Map<String, String> f, Map<String, String> s, int l, int o) { return msgr.get(db, secret, ns, f, s, l, o); }
            public CompletableFuture<Optional<Integer>> count(String ns, Map<String, String> f, int l, int o) { return msgr.count(db, secret, ns, f, l, o); }
            public CompletableFuture<Void> set(String ns, String key, String json) { return msgr.set(db, secret, ns, key, json); }
            public CompletableFuture<Void> delete(String ns, String key) { return msgr.delete(db, secret, ns, key); }
            public void registerModel(String ns, String type, Map<String, ModelField> fields) {} // No-op for handler, done in Manager
            public void shutdown() {}
            public boolean flushAndAwait(long timeout) { return true; }
        };
    }

    // --- Logging Wrappers ---
    protected void logInfo(String msg) { System.out.println("[ADataManager] " + msg); }
    protected void logWarn(String msg) { System.err.println("[ADataManager] WARN: " + msg); }
    protected void logSevere(String msg) { System.err.println("[ADataManager] ERROR: " + msg); }
}