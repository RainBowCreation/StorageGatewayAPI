package net.rainbowcreation.storage.api;

import net.rainbowcreation.storage.api.annotations.EnableBulkWrite;
import net.rainbowcreation.storage.api.annotations.QLQuery;
import net.rainbowcreation.storage.api.utils.SchemaScanner;

import java.lang.reflect.Modifier;

import java.time.Duration;

import java.util.*;
import java.util.concurrent.*;

public interface StorageClient {
    <T> CompletableFuture<Optional<T>> get(String namespace, String key, Class<T> type);

    <T> CompletableFuture<Optional<T>> get(String namespace, Map<String, String> filters, Class<T> type);

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
        return  get(ns, filters, type).join().orElse(null);
    }

    void registerModel(String ns, String typeName, Map<String, ModelField> fields);

    default void registerClass(Class<?> clazz) {
        QLQuery qlq = clazz.getAnnotation(QLQuery.class);
        if (qlq != null) {
            Map<String, ModelField> fields = SchemaScanner.scan(clazz);
            this.registerModel(qlq.namespace(), qlq.typeName(), fields);
            return;
        }

        EnableBulkWrite ebw = clazz.getAnnotation(EnableBulkWrite.class);
        if (ebw != null) {
            this.registerBulkWrite(clazz);
            return;
        }
    }

    default void registerClass(String namespace, Class<?> clazz) { // allow ns override
        QLQuery annotation = clazz.getAnnotation(QLQuery.class);
        if (annotation != null) {
            Map<String, ModelField> fields = SchemaScanner.scan(clazz);
            String ns = namespace;
            if (namespace == null) ns = annotation.namespace();
            this.registerModel(ns, annotation.typeName(), fields);
        }

        EnableBulkWrite ebw = clazz.getAnnotation(EnableBulkWrite.class);
        if (ebw != null) {
            this.registerBulkWrite(clazz);
        }
    }

    default void registerBulkWrite(Class<?> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return;
        }

        // Calculate Bulk Settings
        int batchSize = 100;       // Default rule
        long flushMillis = 500;    // Default rule
        boolean immediate = false;

        if (clazz.isAnnotationPresent(EnableBulkWrite.class)) {
            EnableBulkWrite ebw = clazz.getAnnotation(EnableBulkWrite.class);
            batchSize = ebw.batchSize();
            flushMillis = ebw.flushIntervalMillis();
            immediate = ebw.immediate();
        }

        //todo register bulk write settings
    }
}