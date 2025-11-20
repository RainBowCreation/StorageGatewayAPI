package net.rainbowcreation.storage.api;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;

public interface StorageClient {
    <T> CompletableFuture<Optional<T>> get(String namespace, String key, Class<T> type);

    <T> CompletableFuture<Void> set(String namespace, String key, T value);

    default <T> CompletableFuture<T> getAsyncNullable(String ns, String key, Class<T> type) {
        return get(ns, key, type).thenApply(opt -> opt.orElse(null));
    }

    // Blocking with timeout (return null if not found)
    default <T> T getBlocking(String ns, String key, Class<T> type, Duration timeout) {
        try {
            Optional<T> opt = get(ns, key, type).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return opt.orElse(null);
        } catch (TimeoutException e) {
            return null; // timed out
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException ee) {
            // bubble up original cause as unchecked
            throw new CompletionException(ee.getCause());
        }
    }

    // Blocking without timeout (will wait indefinitely)
    default <T> T getBlocking(String ns, String key, Class<T> type) {
        return get(ns, key, type).join().orElse(null);
    }
}