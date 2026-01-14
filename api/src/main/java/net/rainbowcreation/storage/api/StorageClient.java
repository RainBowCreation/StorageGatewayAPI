package net.rainbowcreation.storage.api;

import net.rainbowcreation.storage.api.annotations.QLQuery;
import net.rainbowcreation.storage.api.utils.SchemaScanner;

import java.time.Duration;

import java.util.*;
import java.util.concurrent.*;

public interface StorageClient {
    <T> CompletableFuture<Optional<T>> get(String namespace, String key, Class<T> type);

    default <T> CompletableFuture<Optional<List<T>>> get(String ns, Map<String, String> filters, Class<T> type) {
        return get(ns, filters, null, 1000, 0, type);
    }
    default <T> CompletableFuture<Optional<List<T>>> get(String ns, Map<String, String> filters, int limit, Class<T> type) {
        return get(ns, filters, null, limit, 0, type);
    }
    default <T> CompletableFuture<Optional<List<T>>> get(String ns, Map<String, String> filters, int limit, int offset, Class<T> type) {
        return get(ns, filters, null, limit, offset, type);
    }
    default <T> CompletableFuture<Optional<List<T>>> get(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type) {
        return get(ns, filters, selections, 1000, 0, type);
    }
    default <T> CompletableFuture<Optional<List<T>>> get(String ns, Map<String, String> filters, Map<String, String> selections, int limit, Class<T> type) {
        return get(ns, filters, selections, limit, 0, type);
    }
    // gets filters limited to 1000 results with 0 offset, limit -1 mean unlimited
    <T> CompletableFuture<Optional<List<T>>> get(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset, Class<T> type);

    default CompletableFuture<Optional<Integer>> count(String ns, Map<String, String> filters) {
        return count(ns, filters, 1000, 0);
    }
    default CompletableFuture<Optional<Integer>> count(String ns, Map<String, String> filters, int limit) {
        return count(ns, filters, limit, 0);
    }
    // count filters limited to 1000 results with 0 offset, limit -1 mean unlimited
    CompletableFuture<Optional<Integer>> count(String ns, Map<String, String> filters, int limit, int offset);

    CompletableFuture<Void> set(String namespace, String key, Object value);

    CompletableFuture<Void> delete(String namespace, String key);

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

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Class<T> type, Duration timeout) {
        return getBlocking(ns, filters, null, 1000, 0, type, timeout);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Class<T> type) {
        return getBlocking(ns, filters, null, 1000, 0, type);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, int limit, Class<T> type, Duration timeout) {
        return getBlocking(ns, filters, null, limit, 0, type, timeout);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, int limit, Class<T> type) {
        return getBlocking(ns, filters, null, limit, 0, type);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, int limit, int offset, Class<T> type, Duration timeout) {
        return getBlocking(ns, filters, null, limit, offset, type, timeout);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, int limit, int offset, Class<T> type) {
        return getBlocking(ns, filters, null, limit, offset, type);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type, Duration timeout) {
        return getBlocking(ns, filters, selections, 1000, 0, type, timeout);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type) {
        return getBlocking(ns, filters, selections, 1000, 0, type);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, int limit, Class<T> type, Duration timeout) {
        return getBlocking(ns, filters, selections, limit, 0, type, timeout);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, int limit, Class<T> type) {
        return getBlocking(ns, filters, selections, limit, 0, type);
    }

    // Blocking without timeout (will wait indefinitely)
    default <T> T getBlocking(String ns, String key, Class<T> type) {
        return get(ns, key, type).join().orElse(null);
    }

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset, Class<T> type, Duration timeout) {
        try {
            Optional<List<T>> opt = get(ns, filters, selections, limit, offset, type).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
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

    default <T> List<T> getBlocking(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset, Class<T> type) {
        return get(ns, filters, selections, limit, offset, type).join().orElse(null);
    }

    default Integer countBlocking(String ns, Map<String, String> filters, Duration timeout) {
        return countBlocking(ns, filters, 1000, 0, timeout);
    }
    default Integer countBlocking(String ns, Map<String, String> filters) {
        return countBlocking(ns, filters, 1000, 0);
    }
    default Integer countBlocking(String ns, Map<String, String> filters, int limit, Duration timeout) {
        return countBlocking(ns, filters, limit, 0, timeout);
    }
    default Integer countBlocking(String ns, Map<String, String> filters, int limit) {
        return countBlocking(ns, filters, limit, 0);
    }

    default Integer countBlocking(String ns, Map<String, String> filters, int limit, int offset, Duration timeout) {
        try {
            Optional<Integer> opt = count(ns, filters, limit, offset).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
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

    default Integer countBlocking(String ns, Map<String, String> filters, int limit, int offset) {
        return count(ns, filters, limit, offset).join().orElse(null);
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