package net.rainbowcreation.storage.api.template;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IDataManager {
    <T> T get(String ns, String key, Class<T> type);
    // optional get with filters and selections
    default String get(String ns, Map<String, String> filters) {
        System.out.println("[IDataManager] get with filters is not implemented, required @Override");
        return null;
    }
    default String get(String ns, Map<String, String> filters, Map<String, String> selections) {
        System.out.println("[IDataManager] get with filters is not implemented, required @Override");
        return null;
    }
    default  <T> List<T> get(String ns, Map<String, String> filters, Class<T> type) {
        System.out.println("[IDataManager] get with filters is not implemented, required @Override");
        return null;
    }
    default <T> List<T> get(String ns, Map<String, String> filters, Map<String, String> selections, Class<T> type) {
        System.out.println("[IDataManager] get with filters is not implemented, required @Override");
        return null;
    }
    void set(String ns, String key, Object value);
    // optional delete set null, can be overridden
    default void delete(String ns, String key) {
        set(ns, key, null);
    }
    // optional exists, can be overridden
    default boolean exists(String ns, String key) {
        return get(ns, key, Object.class) != null;
    }

    default <T> T getOrInit(String ns, String key, Class<T> type, Supplier<T> defSupplier) {
        T val = get(ns, key, type);
        if (val != null) return val;

        T def = defSupplier.get();
        if (def != null) {
            set(ns, key, def);
        }
        return def;
    }

    // --- Byte ---
    default Byte getByte(String ns, String key) { return get(ns, key, Byte.class); }
    default void setByte(String ns, String key, byte val) { set(ns, key, val); }
    default byte getByteOrInit(String ns, String key, byte def) {
        return getOrInit(ns, key, Byte.class, () -> def);
    }

    // --- String ---
    default String getString(String ns, String key) { return get(ns, key, String.class); }
    default void setString(String ns, String key, String val) { set(ns, key, val); }
    default String getStringOrInit(String ns, String key, String def) {
        return getOrInit(ns, key, String.class, () -> def);
    }

    // --- Integer ---
    default Integer getInt(String ns, String key) { return get(ns, key, Integer.class); }
    default void setInt(String ns, String key, int val) { set(ns, key, val); }
    default int getIntOrInit(String ns, String key, int def) {
        return getOrInit(ns, key, Integer.class, () -> def);
    }

    // --- Long ---
    default Long getLong(String ns, String key) { return get(ns, key, Long.class); }
    default void setLong(String ns, String key, long val) { set(ns, key, val); }
    default long getLongOrInit(String ns, String key, long def) {
        return getOrInit(ns, key, Long.class, () -> def);
    }

    // --- Boolean ---
    default Boolean getBool(String ns, String key) { return get(ns, key, Boolean.class); }
    default void setBool(String ns, String key, boolean val) { set(ns, key, val); }
    default boolean getBoolOrInit(String ns, String key, boolean def) {
        return getOrInit(ns, key, Boolean.class, () -> def);
    }

    // --- Double ---
    default Double getDouble(String ns, String key) { return get(ns, key, Double.class); }
    default void setDouble(String ns, String key, double val) { set(ns, key, val); }
    default double getDoubleOrInit(String ns, String key, double def) {
        return getOrInit(ns, key, Double.class, () -> def);
    }

    default <T, R> R derive(String ns, String key, Class<T> rawType, Function<T, R> mapper) {
        T raw = get(ns, key, rawType);
        return (raw == null) ? null : mapper.apply(raw);
    }

    // --- UUID ---
    default UUID getUUID(String ns, String key) {
        return derive(ns, key, String.class, UUID::fromString);
    }
    default void setUUID(String ns, String key, UUID val) {
        set(ns, key, val.toString());
    }
    default UUID getUUIDOrInit(String ns, String key) {
        UUID existing = getUUID(ns, key);
        if (existing != null) return existing;
        UUID newId = UUID.randomUUID();
        setUUID(ns, key, newId);
        return newId;
    }

    // --- Instant ---
    default Instant getInstant(String ns, String key) {
        return derive(ns, key, Long.class, Instant::ofEpochMilli);
    }
    default void setInstant(String ns, String key, Instant val) {
        set(ns, key, val.toEpochMilli());
    }
    default Instant getInstantOrInit(String ns, String key) {
        Instant existing = getInstant(ns, key);
        if (existing != null) return existing;
        Instant now = Instant.now();
        setInstant(ns, key, now);
        return now;
    }

    // --- Enum ---
    default <E extends Enum<E>> E getEnum(String ns, String key, Class<E> type) {
        return derive(ns, key, String.class, s -> Enum.valueOf(type, s));
    }
    default <E extends Enum<E>> void setEnum(String ns, String key, E val) {
        set(ns, key, val.name());
    }
    default <E extends Enum<E>> E getEnumOrInit(String ns, String key, Class<E> type, E def) {
        E val = getEnum(ns, key, type);
        if (val != null) return val;
        setEnum(ns, key, def);
        return def;
    }

    // --- BigDecimal ---
    default BigDecimal getDecimal(String ns, String key) {
        return derive(ns, key, String.class, BigDecimal::new);
    }
    default void setDecimal(String ns, String key, BigDecimal val) {
        set(ns, key, val.toPlainString());
    }
    default BigDecimal getDecimalOrInit(String ns, String key, BigDecimal def) {
        BigDecimal val = getDecimal(ns, key);
        if(val != null) return val;
        setDecimal(ns, key, def);
        return def;
    }

    @SuppressWarnings("unchecked")
    default <T> List<T> getList(String ns, String key) {
        return get(ns, key, List.class);
    }
    default <T> void setList(String ns, String key, List<T> list) {
        set(ns, key, list);
    }
    @SuppressWarnings("unchecked")
    default <T> List<T> getListOrInit(String ns, String key) {
        return getOrInit(ns, key, List.class, ArrayList::new);
    }

    // --- Map ---
    @SuppressWarnings("unchecked")
    default <K, V> Map<K, V> getMap(String ns, String key) {
        return get(ns, key, Map.class);
    }
    default <K, V> void setMap(String ns, String key, Map<K, V> map) {
        set(ns, key, map);
    }
    @SuppressWarnings("unchecked")
    default <K, V> Map<K, V> getMapOrInit(String ns, String key) {
        return getOrInit(ns, key, Map.class, HashMap::new);
    }

    // --- Set ---
    @SuppressWarnings("unchecked")
    default <T> Set<T> getSet(String ns, String key) {
        return get(ns, key, Set.class);
    }
    default <T> void setSet(String ns, String key, Set<T> set) {
        set(ns, key, set);
    }
    @SuppressWarnings("unchecked")
    default <T> Set<T> getSetOrInit(String ns, String key) {
        return getOrInit(ns, key, Set.class, HashSet::new);
    }
}