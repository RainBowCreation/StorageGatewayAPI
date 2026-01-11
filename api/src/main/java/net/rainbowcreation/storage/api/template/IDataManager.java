package net.rainbowcreation.storage.api.template;

import java.math.BigDecimal;

import java.time.Instant;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IDataManager {
    <T> T get(String ns, String key, Class<T> type);

    void set(String ns, String key, Object value);

    void delete(String ns, String key);

    boolean exists(String ns, String key);

    default <T> T getOrInit(String ns, String key, Class<T> type, Supplier<T> supplier) {
        T v = get(ns, key, type);
        if (v != null) return v;
        T def = supplier.get();
        set(ns, key, def);
        return def;
    }

    Map<Class<?>, Supplier<?>> DEFAULTS = Stream.of(
            new Object[]{ Map.class, (Supplier<?>) HashMap::new },
            new Object[]{ List.class, (Supplier<?>) ArrayList::new },
            new Object[]{ Set.class, (Supplier<?>) HashSet::new }
    ).collect(Collectors.toMap(
            data -> (Class<?>) data[0],
            data -> (Supplier<?>) data[1]
    ));

    default <T> T getOrInit(String ns, String key, Class<T> type) {
        Supplier<?> sup = DEFAULTS.get(type);
        if (sup == null) {
            throw new IllegalArgumentException(
                    "No default supplier registered for " + type.getName() +
                            ". Use getOrInit(ns,key,type,supplier) overload.");
        }
        return getOrInit(ns, key, type, (Supplier<T>) sup);
    }

    default String  getString (String ns, String key) { return get(ns, key, String.class); }
    default Boolean getBool   (String ns, String key) { return get(ns, key, Boolean.class); }
    default Integer getInt    (String ns, String key) { return get(ns, key, Integer.class); }
    default Long    getLong   (String ns, String key) { return get(ns, key, Long.class); }
    default Double  getDouble (String ns, String key) { return get(ns, key, Double.class); }
    default void setString (String ns, String key, String  val){ set(ns, key, val); }
    default void setBool   (String ns, String key, boolean val){ set(ns, key, val); }
    default void setInt    (String ns, String key, int     val){ set(ns, key, val); }
    default void setLong   (String ns, String key, long    val){ set(ns, key, val); }
    default void setDouble (String ns, String key, double  val){ set(ns, key, val); }
    default String  getStringOrInit (String ns, String key, String  def){ return getOrInit(ns, key, String.class,  () -> def); }
    default boolean getBoolOrInit   (String ns, String key, boolean def){ return getOrInit(ns, key, Boolean.class, () -> def); }
    default int     getIntOrInit    (String ns, String key, int     def){ return getOrInit(ns, key, Integer.class, () -> def); }
    default long    getLongOrInit   (String ns, String key, long    def){ return getOrInit(ns, key, Long.class,    () -> def); }
    default double  getDoubleOrInit (String ns, String key, double  def){ return getOrInit(ns, key, Double.class,  () -> def); }
    default BigDecimal getDecimal(String ns, String key){
        String s = getString(ns, key);
        return (s == null) ? null : new BigDecimal(s);
    }
    default void setDecimal(String ns, String key, BigDecimal v){
        setString(ns, key, v.toPlainString());
    }
    default BigDecimal getDecimalOrInit(String ns, String key, BigDecimal def){
        return getOrInit(ns, key, BigDecimal.class, () -> def);
    }
    default byte[] getBytes(String ns, String key){ return get(ns, key, byte[].class); }
    default void   setBytes(String ns, String key, byte[] val){ set(ns, key, val); }
    default byte[] getBytesOrInit(String ns, String key, Supplier<byte[]> supplier){
        return getOrInit(ns, key, byte[].class, supplier);
    }
    default UUID getUUID(String ns, String key){
        String s = getString(ns, key);
        return (s == null) ? null : UUID.fromString(s);
    }
    default void setUUID(String ns, String key, UUID uuid){ setString(ns, key, uuid.toString()); }
    default UUID getUUIDOrInit(String ns, String key){
        UUID u = getUUID(ns, key);
        if (u != null) return u;
        u = UUID.randomUUID();
        setUUID(ns, key, u);
        return u;
    }
    default Instant getInstant(String ns, String key){
        Long v = getLong(ns, key);
        return (v == null) ? null : Instant.ofEpochMilli(v);
    }
    default void setInstant(String ns, String key, Instant ts){
        setLong(ns, key, ts.toEpochMilli());
    }
    default Instant getInstantOrInitNow(String ns, String key){
        Instant t = getInstant(ns, key);
        if (t != null) return t;
        t = Instant.now();
        setInstant(ns, key, t);
        return t;
    }

    default <E extends Enum<E>> E getEnum(String ns, String key, Class<E> enumType){
        String s = getString(ns, key);
        return (s == null) ? null : Enum.valueOf(enumType, s);
    }
    default <E extends Enum<E>> void setEnum(String ns, String key, E value){
        setString(ns, key, value.name());
    }
    default <E extends Enum<E>> E getEnumOrInit(String ns, String key, Class<E> enumType, E def){
        E v = getEnum(ns, key, enumType);
        if (v != null) return v;
        setEnum(ns, key, def);
        return def;
    }
    @SuppressWarnings("unchecked")
    default <K,V> void putMapEntry(String ns, String key, K k, V v){
        Map<K,V> m = (Map<K,V>) getOrInit(ns, key, Map.class);
        m.put(k, v);
        set(ns, key, m);
    }
    @SuppressWarnings("unchecked")
    default <K,V> void putAllMap(String ns, String key, Map<K,V> delta){
        Map<K,V> m = (Map<K,V>) getOrInit(ns, key, Map.class);
        m.putAll(delta);
        set(ns, key, m);
    }
    @SuppressWarnings("unchecked")
    default <K> void removeMapKey(String ns, String key, K k){
        Map<K,?> m = (Map<K,?>) getOrInit(ns, key, Map.class);
        m.remove(k);
        set(ns, key, m);
    }
    @SuppressWarnings("unchecked")
    default <E> void appendList(String ns, String key, E elem){
        List<E> l = (List<E>) getOrInit(ns, key, List.class);
        l.add(elem);
        set(ns, key, l);
    }
    @SuppressWarnings("unchecked")
    default <E> void removeFromList(String ns, String key, E elem){
        List<E> l = (List<E>) getOrInit(ns, key, List.class);
        l.remove(elem);
        set(ns, key, l);
    }
    @SuppressWarnings("unchecked")
    default <E> void addToSet(String ns, String key, E elem){
        Set<E> s = (Set<E>) getOrInit(ns, key, Set.class);
        s.add(elem);
        set(ns, key, s);
    }
    @SuppressWarnings("unchecked")
    default <E> void removeFromSet(String ns, String key, E elem){
        Set<E> s = (Set<E>) getOrInit(ns, key, Set.class);
        s.remove(elem);
        set(ns, key, s);
    }
    default long incrLong(String ns, String key, long delta){
        Long cur = getLongOrInit(ns, key, 0L);
        long next = (cur == null ? 0L : cur) + delta;
        setLong(ns, key, next);
        return next;
    }
}