package net.rainbowcreation.storage.api;

import net.rainbowcreation.storage.api.annotations.QLQuery;
import net.rainbowcreation.storage.api.utils.SchemaScanner;

import java.time.Duration;

import java.util.*;
import java.util.concurrent.*;

public interface StorageClient {
    <T> CompletableFuture<Optional<T>> get(String namespace, String key, Class<T> type);

    <T> CompletableFuture<Optional<T>> get(String namespace, Map<String, String> filters, Class<T> type);

    // Get raw JSON string with filters and selections, selections can be null
    CompletableFuture<Optional<String>> get(String namespace, Map<String, String> filters, Map<String, String> selections);
    // Get list of objects with filters and selections, selections can be null
    <T> CompletableFuture<Optional<List<T>>> get(String namespace, Map<String, String> filters, Map<String, String> selections, Class<T> type);

    <T> CompletableFuture<Void> set(String namespace, String key, T value);

    <T> CompletableFuture<Void> delete(String namespace, String key);

    default <T> CompletableFuture<T> getAsyncNullable(String ns, String key, Class<T> type) {
        return get(ns, key, type).thenApply(opt -> opt.orElse(null));
    }

    // Blocking with timeout (return null if not found)
    default <T> T getBlocking(String ns, String key, Class<T> type, Duration timeout) {
        try {
            Optional<T> opt = get(ns, key, type).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return opt.orElse(null);
        } catch (TimeoutException e) {
            return null;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            throw new CompletionException(ee.getCause());
        }
        return null;
    }

    // Blocking without timeout (will wait indefinitely)
    default <T> T getBlocking(String ns, String key, Class<T> type) {
        return get(ns, key, type).join().orElse(null);
    }

    default <T> T getBlocking(String ns, Map<String, String> filters, Class<T> type, Duration timeout) {
        try {
            Optional<T> opt = get(ns, filters, type).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return opt.orElse(null);
        } catch (TimeoutException e) {
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            throw new CompletionException(ee.getCause());
        }
        return null;
    }

    default <T> T getBlocking(String ns, Map<String, String> filters, Class<T> type) {
        return get(ns, filters, type).join().orElse(null);
    }

    default String getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, Duration timeout) {
        try {
            Optional<String> opt = get(ns, filters, selections).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return opt.orElse(null);
        } catch (TimeoutException e) {
            return null;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            throw new CompletionException(ee.getCause());
        }
        return null;
    }

    default String getBlocking(String ns, Map<String, String> filters, Map<String, String> selections) {
        return get(ns, filters, selections).join().orElse(null);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type, Duration timeout) {
        try {
            Optional<List<T>> opt = get(ns, filters, selections, type).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return opt.orElse(null);
        } catch (TimeoutException e) {
            return null;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ee) {
            throw new CompletionException(ee.getCause());
        }
        return null;
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type) {
        return get(ns, filters, selections, type).join().orElse(null);
    }

    void registerModel(String ns, String typeName, Map<String, ModelField> fields);

    default void registerClass(Class<?> clazz) {
        QLQuery qlq = clazz.getAnnotation(QLQuery.class);
        if (qlq != null) {
            Map<String, ModelField> fields = SchemaScanner.scan(clazz);
            this.registerModel(qlq.namespace(), qlq.typeName(), fields);
            return;
        }

        //another annotations can be added here
    }

    default void registerClass(String namespace, Class<?> clazz) { // allow ns override
        QLQuery annotation = clazz.getAnnotation(QLQuery.class);
        if (annotation != null) {
            Map<String, ModelField> fields = SchemaScanner.scan(clazz);
            String ns = namespace;
            if (namespace == null) ns = annotation.namespace();
            this.registerModel(ns, annotation.typeName(), fields);
        }

        //another annotations can be added here
    }
}