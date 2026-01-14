package net.rainbowcreation.storage.api.common;

import net.rainbowcreation.storage.api.ModelField;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GatewayHandler {
    CompletableFuture<Optional<String>> get(String ns, String key);

    default CompletableFuture<Optional<String>> get(String ns, Map<String, String> filters) {
        return get(ns, filters, null, 1000, 0);
    }
    default CompletableFuture<Optional<String>> get(String ns, Map<String, String> filters, int limit) {
        return get(ns, filters, null, limit, 0);
    }
    default CompletableFuture<Optional<String>> get(String ns, Map<String, String> filters, int limit, int offset) {
        return get(ns, filters, null, limit, offset);
    }
    default CompletableFuture<Optional<String>> get(String ns, Map<String, String> filters, Map<String, String> selections) {
        return get(ns, filters, selections, 1000, 0);
    }
    default CompletableFuture<Optional<String>> get(String ns, Map<String, String> filters, Map<String, String> selections, int limit) {
        return get(ns, filters, selections, limit, 0);
    }
    CompletableFuture<Optional<String>> get(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset);

    default CompletableFuture<Optional<Integer>> count(String ns, Map<String, String> filters) {
        return count(ns, filters, 1000, 0);
    }
    default CompletableFuture<Optional<Integer>> count(String ns, Map<String, String> filters, int limit) {
        return count(ns, filters, limit, 0);
    }
    CompletableFuture<Optional<Integer>> count(String ns, Map<String, String> filters, int limit, int offset);

    CompletableFuture<Void> set(String ns, String key, String json);
    CompletableFuture<Void> delete(String ns, String key);
    void registerModel(String ns, String typeName, Map<String, ModelField> fields);
    boolean flushAndAwait(long timeoutMs);
    void shutdown();
}