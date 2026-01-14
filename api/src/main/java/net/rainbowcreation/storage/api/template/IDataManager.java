package net.rainbowcreation.storage.api.template;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IDataManager {
    <T> T get(String ns, String key, Class<T> type);
    <T> CompletableFuture<T> getAsync(String ns, String key, Class<T> type);

    void set(String ns, String key, Object value);
    CompletableFuture<Void> setAsync(String ns, String key, Object value);

    default void delete(String ns, String key) {
        set(ns, key, null);
    }
    default CompletableFuture<Void> deleteAsync(String ns, String key) {
        return setAsync(ns, key, null);
    }

    default boolean exists(String ns, String key) {
        return get(ns, key, Object.class) != null;
    }
    default CompletableFuture<Boolean> existsAsync(String ns, String key) {
        return getAsync(ns, key, Object.class).thenApply(obj -> obj != null);
    }

    // --- Blocking Defaults ---
    default <T> List<T> get(String ns, Map<String, String> filters, Class<T> type) { return get(ns, filters, null, 1000, 0, type); }
    default <T> List<T> get(String ns, Map<String, String> filters, int limit, Class<T> type) { return get(ns, filters, null, limit, 0, type); }
    default <T> List<T> get(String ns, Map<String, String> filters, int limit, int offset, Class<T> type) { return get(ns, filters, null, limit, offset, type); }
    default <T> List<T> get(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type) { return get(ns, filters, selections, 1000, 0, type); }
    default <T> List<T> get(String ns, Map<String, String> filters, Map<String, String> selections, int limit, Class<T> type) { return get(ns, filters, selections, limit, 0, type); }

    // Core Blocking Implementation
    default <T> List<T> get(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset, Class<T> type) {
        throw new UnsupportedOperationException("Not implemented, require @Override");
    }

    // --- Async Defaults ---
    default <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, Class<T> type) { return getAsync(ns, filters, null, 1000, 0, type); }
    default <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, int limit, Class<T> type) { return getAsync(ns, filters, null, limit, 0, type); }
    default <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, int limit, int offset, Class<T> type) { return getAsync(ns, filters, null, limit, offset, type); }
    default <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type) { return getAsync(ns, filters, selections, 1000, 0, type); }
    default <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, Map<String, String> selections, int limit, Class<T> type) { return getAsync(ns, filters, selections, limit, 0, type); }

    // Core Async Implementation
    default <T> CompletableFuture<List<T>> getAsync(String ns, Map<String, String> filters, Map<String, String> selections, int limit, int offset, Class<T> type) {
        throw new UnsupportedOperationException("Not implemented, require @Override");
    }

    default Integer count(String ns, Map<String, String> filters) { return count(ns, filters, 1000, 0); }
    default Integer count(String ns, Map<String, String> filters, int limit) { return count(ns, filters, limit, 0); }

    // Core Blocking Implementation
    default Integer count(String ns, Map<String, String> filters, int limit, int offset) {
        throw new UnsupportedOperationException("Not implemented, require @Override");
    }

    // --- Async Defaults ---
    default CompletableFuture<Integer> countAsync(String ns, Map<String, String> filters) { return countAsync(ns, filters, 1000, 0); }
    default CompletableFuture<Integer> countAsync(String ns, Map<String, String> filters, int limit) { return countAsync(ns, filters, limit, 0); }

    // Core Async Implementation
    default CompletableFuture<Integer> countAsync(String ns, Map<String, String> filters, int limit, int offset) {
        throw new UnsupportedOperationException("Not implemented, require @Override");
    }

    default <T> T getOrInit(String ns, String key, Class<T> type, Supplier<T> defSupplier) {
        T val = get(ns, key, type);
        if (val != null) return val;
        T def = defSupplier.get();
        if (def != null) set(ns, key, def);
        return def;
    }
    default <T> CompletableFuture<T> getOrInitAsync(String ns, String key, Class<T> type, Supplier<T> defSupplier) {
        return getAsync(ns, key, type).thenCompose(val -> {
            if (val != null) return CompletableFuture.completedFuture(val);
            T def = defSupplier.get();
            if (def != null) {
                return setAsync(ns, key, def).thenApply(v -> def);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    default <T, R> R derive(String ns, String key, Class<T> rawType, Function<T, R> mapper) {
        T raw = get(ns, key, rawType);
        return (raw == null) ? null : mapper.apply(raw);
    }

    default <T, R> CompletableFuture<R> deriveAsync(String ns, String key, Class<T> rawType, Function<T, R> mapper) {
        return getAsync(ns, key, rawType).thenApply(raw -> (raw == null) ? null : mapper.apply(raw));
    }

    // --- Byte ---
    default Byte getByte(String ns, String key) { return get(ns, key, Byte.class); }
    default CompletableFuture<Byte> getByteAsync(String ns, String key) { return getAsync(ns, key, Byte.class); }

    default void setByte(String ns, String key, byte val) { set(ns, key, val); }
    default CompletableFuture<Void> setByteAsync(String ns, String key, byte val) { return setAsync(ns, key, val); }

    default byte getByteOrInit(String ns, String key, byte def) { return getOrInit(ns, key, Byte.class, () -> def); }
    default CompletableFuture<Byte> getByteOrInitAsync(String ns, String key, byte def) { return getOrInitAsync(ns, key, Byte.class, () -> def); }

    // --- String ---
    default String getString(String ns, String key) { return get(ns, key, String.class); }
    default CompletableFuture<String> getStringAsync(String ns, String key) { return getAsync(ns, key, String.class); }

    default void setString(String ns, String key, String val) { set(ns, key, val); }
    default CompletableFuture<Void> setStringAsync(String ns, String key, String val) { return setAsync(ns, key, val); }

    default String getStringOrInit(String ns, String key, String def) { return getOrInit(ns, key, String.class, () -> def); }
    default CompletableFuture<String> getStringOrInitAsync(String ns, String key, String def) { return getOrInitAsync(ns, key, String.class, () -> def); }

    // --- Integer ---
    default Integer getInt(String ns, String key) { return get(ns, key, Integer.class); }
    default CompletableFuture<Integer> getIntAsync(String ns, String key) { return getAsync(ns, key, Integer.class); }

    default void setInt(String ns, String key, int val) { set(ns, key, val); }
    default CompletableFuture<Void> setIntAsync(String ns, String key, int val) { return setAsync(ns, key, val); }

    default int getIntOrInit(String ns, String key, int def) { return getOrInit(ns, key, Integer.class, () -> def); }
    default CompletableFuture<Integer> getIntOrInitAsync(String ns, String key, int def) { return getOrInitAsync(ns, key, Integer.class, () -> def); }

    // --- Long ---
    default Long getLong(String ns, String key) { return get(ns, key, Long.class); }
    default CompletableFuture<Long> getLongAsync(String ns, String key) { return getAsync(ns, key, Long.class); }

    default void setLong(String ns, String key, long val) { set(ns, key, val); }
    default CompletableFuture<Void> setLongAsync(String ns, String key, long val) { return setAsync(ns, key, val); }

    default long getLongOrInit(String ns, String key, long def) { return getOrInit(ns, key, Long.class, () -> def); }
    default CompletableFuture<Long> getLongOrInitAsync(String ns, String key, long def) { return getOrInitAsync(ns, key, Long.class, () -> def); }

    // --- Boolean ---
    default Boolean getBool(String ns, String key) { return get(ns, key, Boolean.class); }
    default CompletableFuture<Boolean> getBoolAsync(String ns, String key) { return getAsync(ns, key, Boolean.class); }

    default void setBool(String ns, String key, boolean val) { set(ns, key, val); }
    default CompletableFuture<Void> setBoolAsync(String ns, String key, boolean val) { return setAsync(ns, key, val); }

    default boolean getBoolOrInit(String ns, String key, boolean def) { return getOrInit(ns, key, Boolean.class, () -> def); }
    default CompletableFuture<Boolean> getBoolOrInitAsync(String ns, String key, boolean def) { return getOrInitAsync(ns, key, Boolean.class, () -> def); }

    // --- Double ---
    default Double getDouble(String ns, String key) { return get(ns, key, Double.class); }
    default CompletableFuture<Double> getDoubleAsync(String ns, String key) { return getAsync(ns, key, Double.class); }

    default void setDouble(String ns, String key, double val) { set(ns, key, val); }
    default CompletableFuture<Void> setDoubleAsync(String ns, String key, double val) { return setAsync(ns, key, val); }

    default double getDoubleOrInit(String ns, String key, double def) { return getOrInit(ns, key, Double.class, () -> def); }
    default CompletableFuture<Double> getDoubleOrInitAsync(String ns, String key, double def) { return getOrInitAsync(ns, key, Double.class, () -> def); }

    // --- UUID ---
    default UUID getUUID(String ns, String key) { return derive(ns, key, String.class, UUID::fromString); }
    default CompletableFuture<UUID> getUUIDAsync(String ns, String key) { return deriveAsync(ns, key, String.class, UUID::fromString); }

    default void setUUID(String ns, String key, UUID val) { set(ns, key, val.toString()); }
    default CompletableFuture<Void> setUUIDAsync(String ns, String key, UUID val) { return setAsync(ns, key, val.toString()); }

    default UUID getUUIDOrInit(String ns, String key) {
        return getOrInit(ns, key, UUID.class, UUID::randomUUID); // UUID implementation specific logic
    }
    // Specialized async init logic for UUID to avoid complex lambda casting
    default CompletableFuture<UUID> getUUIDOrInitAsync(String ns, String key) {
        return getUUIDAsync(ns, key).thenCompose(val -> {
            if (val != null) return CompletableFuture.completedFuture(val);
            UUID newId = UUID.randomUUID();
            return setUUIDAsync(ns, key, newId).thenApply(v -> newId);
        });
    }

    // --- Instant ---
    default Instant getInstant(String ns, String key) { return derive(ns, key, Long.class, Instant::ofEpochMilli); }
    default CompletableFuture<Instant> getInstantAsync(String ns, String key) { return deriveAsync(ns, key, Long.class, Instant::ofEpochMilli); }

    default void setInstant(String ns, String key, Instant val) { set(ns, key, val.toEpochMilli()); }
    default CompletableFuture<Void> setInstantAsync(String ns, String key, Instant val) { return setAsync(ns, key, val.toEpochMilli()); }

    default Instant getInstantOrInit(String ns, String key) {
        Instant existing = getInstant(ns, key);
        if (existing != null) return existing;
        Instant now = Instant.now();
        setInstant(ns, key, now);
        return now;
    }
    default CompletableFuture<Instant> getInstantOrInitAsync(String ns, String key) {
        return getInstantAsync(ns, key).thenCompose(val -> {
            if (val != null) return CompletableFuture.completedFuture(val);
            Instant now = Instant.now();
            return setInstantAsync(ns, key, now).thenApply(v -> now);
        });
    }

    // --- Enum ---
    default <E extends Enum<E>> E getEnum(String ns, String key, Class<E> type) { return derive(ns, key, String.class, s -> Enum.valueOf(type, s)); }
    default <E extends Enum<E>> CompletableFuture<E> getEnumAsync(String ns, String key, Class<E> type) { return deriveAsync(ns, key, String.class, s -> Enum.valueOf(type, s)); }

    default <E extends Enum<E>> void setEnum(String ns, String key, E val) { set(ns, key, val.name()); }
    default <E extends Enum<E>> CompletableFuture<Void> setEnumAsync(String ns, String key, E val) { return setAsync(ns, key, val.name()); }

    default <E extends Enum<E>> E getEnumOrInit(String ns, String key, Class<E> type, E def) { return getOrInit(ns, key, type, () -> def); } // Note: logic slightly tricky with generic types in default methods, but standard getOrInit works if type passed correctly
    default <E extends Enum<E>> CompletableFuture<E> getEnumOrInitAsync(String ns, String key, Class<E> type, E def) { return getOrInitAsync(ns, key, type, () -> def); }

    // --- BigDecimal ---
    default BigDecimal getDecimal(String ns, String key) { return derive(ns, key, String.class, BigDecimal::new); }
    default CompletableFuture<BigDecimal> getDecimalAsync(String ns, String key) { return deriveAsync(ns, key, String.class, BigDecimal::new); }

    default void setDecimal(String ns, String key, BigDecimal val) { set(ns, key, val.toPlainString()); }
    default CompletableFuture<Void> setDecimalAsync(String ns, String key, BigDecimal val) { return setAsync(ns, key, val.toPlainString()); }

    default BigDecimal getDecimalOrInit(String ns, String key, BigDecimal def) { return getOrInit(ns, key, BigDecimal.class, () -> def); }
    default CompletableFuture<BigDecimal> getDecimalOrInitAsync(String ns, String key, BigDecimal def) { return getOrInitAsync(ns, key, BigDecimal.class, () -> def); }

    // --- Collections ---

    // --- List ---
    @SuppressWarnings("unchecked")
    default <T> List<T> getList(String ns, String key) { return (List<T>) get(ns, key, List.class); }
    @SuppressWarnings("unchecked")
    default <T> CompletableFuture<List<T>> getListAsync(String ns, String key) { return (CompletableFuture<List<T>>) (CompletableFuture<?>) getAsync(ns, key, List.class); }

    default <T> void setList(String ns, String key, List<T> list) { set(ns, key, list); }
    default <T> CompletableFuture<Void> setListAsync(String ns, String key, List<T> list) { return setAsync(ns, key, list); }

    @SuppressWarnings("unchecked")
    default <T> List<T> getListOrInit(String ns, String key) { return (List<T>) getOrInit(ns, key, List.class, ArrayList::new); }
    @SuppressWarnings("unchecked")
    default <T> CompletableFuture<List<T>> getListOrInitAsync(String ns, String key) { return (CompletableFuture<List<T>>) (CompletableFuture<?>) getOrInitAsync(ns, key, List.class, ArrayList::new); }

    // --- Map ---
    @SuppressWarnings("unchecked")
    default <K, V> Map<K, V> getMap(String ns, String key) { return (Map<K, V>) get(ns, key, Map.class); }
    @SuppressWarnings("unchecked")
    default <K, V> CompletableFuture<Map<K, V>> getMapAsync(String ns, String key) { return (CompletableFuture<Map<K, V>>) (CompletableFuture<?>) getAsync(ns, key, Map.class); }

    default <K, V> void setMap(String ns, String key, Map<K, V> map) { set(ns, key, map); }
    default <K, V> CompletableFuture<Void> setMapAsync(String ns, String key, Map<K, V> map) { return setAsync(ns, key, map); }

    @SuppressWarnings("unchecked")
    default <K, V> Map<K, V> getMapOrInit(String ns, String key) { return (Map<K, V>) getOrInit(ns, key, Map.class, HashMap::new); }
    @SuppressWarnings("unchecked")
    default <K, V> CompletableFuture<Map<K, V>> getMapOrInitAsync(String ns, String key) { return (CompletableFuture<Map<K, V>>) (CompletableFuture<?>) getOrInitAsync(ns, key, Map.class, HashMap::new); }

    // --- Set ---
    @SuppressWarnings("unchecked")
    default <T> Set<T> getSet(String ns, String key) { return (Set<T>) get(ns, key, Set.class); }
    @SuppressWarnings("unchecked")
    default <T> CompletableFuture<Set<T>> getSetAsync(String ns, String key) { return (CompletableFuture<Set<T>>) (CompletableFuture<?>) getAsync(ns, key, Set.class); }

    default <T> void setSet(String ns, String key, Set<T> set) { set(ns, key, set); }
    default <T> CompletableFuture<Void> setSetAsync(String ns, String key, Set<T> set) { return setAsync(ns, key, set); }

    @SuppressWarnings("unchecked")
    default <T> Set<T> getSetOrInit(String ns, String key) { return (Set<T>) getOrInit(ns, key, Set.class, HashSet::new); }
    @SuppressWarnings("unchecked")
    default <T> CompletableFuture<Set<T>> getSetOrInitAsync(String ns, String key) { return (CompletableFuture<Set<T>>) (CompletableFuture<?>) getOrInitAsync(ns, key, Set.class, HashSet::new); }
}