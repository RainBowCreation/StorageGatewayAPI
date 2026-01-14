package net.rainbowcreation.storage.api.proxy;

import net.rainbowcreation.storage.api.ModelField;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ProxyMessenger {
    CompletableFuture<Optional<String>> get(String db, String secret, String ns, String key);
    CompletableFuture<Optional<String>> get(String db, String secret, String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset);
    CompletableFuture<Optional<Integer>> count(String db, String secret, String ns, Map<String, String> filters, int limit, int offset);
    void sendRegisterModel(String db, String secret, String ns, String typeName, Map<String, ModelField> fields);
    CompletableFuture<Void> set(String db, String secret, String ns, String key, String json);
    CompletableFuture<Void> delete(String db, String secret, String ns, String key);
    void register();
    void unregister();
}