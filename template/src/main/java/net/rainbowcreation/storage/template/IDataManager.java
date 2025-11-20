package net.rainbowcreation.storage.template;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public interface IDataManager {
    <T> T get(String ns, String key, Class<T> type);

    void set(String ns, String key, Object value);

    boolean exists(String ns, String key);

    <T> T getOrInit(String ns, String key, Class<T> type, Supplier<T> supplier);

    <T> T getOrInit(String ns, String key, Class<T> type);

    String  getString (String ns, String key);
    Boolean getBool   (String ns, String key);
    Integer getInt    (String ns, String key);
    Long    getLong   (String ns, String key);
    Double  getDouble (String ns, String key);

    void setString (String ns, String key, String  val);
    void setBool   (String ns, String key, boolean val);
    void setInt    (String ns, String key, int     val);
    void setLong   (String ns, String key, long    val);
    void setDouble (String ns, String key, double  val);

    String  getStringOrInit (String ns, String key, String  def);
    boolean getBoolOrInit   (String ns, String key, boolean def);
    int     getIntOrInit    (String ns, String key, int     def);
    long    getLongOrInit   (String ns, String key, long    def);
    double  getDoubleOrInit (String ns, String key, double  def);

    BigDecimal getDecimal(String ns, String key);
    void setDecimal(String ns, String key, BigDecimal v);
    BigDecimal getDecimalOrInit(String ns, String key, BigDecimal def);

    byte[] getBytes(String ns, String key);
    void   setBytes(String ns, String key, byte[] val);
    byte[] getBytesOrInit(String ns, String key, Supplier<byte[]> supplier);

    UUID getUUID(String ns, String key);
    void setUUID(String ns, String key, UUID uuid);
    UUID getUUIDOrInit(String ns, String key);

    Instant getInstant(String ns, String key);
    void setInstant(String ns, String key, Instant ts);
    Instant getInstantOrInitNow(String ns, String key);
    <E extends Enum<E>> E getEnum(String ns, String key, Class<E> enumType);
    <E extends Enum<E>> void setEnum(String ns, String key, E value);
    <E extends Enum<E>> E getEnumOrInit(String ns, String key, Class<E> enumType, E def);

    <K,V> Map<K,V> getOrInitMap(String ns, String key);
    <E> List<E> getOrInitList(String ns, String key);
    <E> Set<E> getOrInitSet(String ns, String key);
    void setMap (String ns, String key, Map<?,?>  val);
    void setList(String ns, String key, List<?>   val);
    void setSet (String ns, String key, Set<?>    val);

    void delete(String ns, String key);
    void clearNamespace(String ns);

    <K,V> void putMapEntry(String ns, String key, K k, V v);
    <K,V> void putAllMap(String ns, String key, Map<K,V> delta);
    <K> void removeMapKey(String ns, String key, K k);
    <E> void appendList(String ns, String key, E elem);
    <E> void removeFromList(String ns, String key, E elem);
    <E> void addToSet(String ns, String key, E elem);
    <E> void removeFromSet(String ns, String key, E elem);
    long incrLong(String ns, String key, long delta);
}