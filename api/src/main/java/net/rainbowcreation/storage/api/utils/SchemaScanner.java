package net.rainbowcreation.storage.api.utils;

import net.rainbowcreation.storage.api.ModelField;
import net.rainbowcreation.storage.api.annotations.EnableQuery;
import net.rainbowcreation.storage.api.annotations.QLQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.*;

public class SchemaScanner {

    /**
     * Entry point: Scans user class -> Map of "alias" -> ModelField(path, type)
     */
    public static Map<String, ModelField> scan(Class<?> clazz) {
        QLQuery annotation = clazz.getAnnotation(QLQuery.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class " + clazz.getSimpleName() + " is missing @QLQuery");
        }

        Map<String, ModelField> schema = new LinkedHashMap<>();
        scanRecursive(clazz, "", "$", schema, 0);
        return schema;
    }

    private static void scanRecursive(Class<?> clazz, String namePrefix, String pathPrefix, Map<String, ModelField> schema, int depth) {
        if (depth > 4) return;

        for (Field field : clazz.getDeclaredFields()) {
            if (shouldIgnore(field)) continue;
            if (!field.isAnnotationPresent(EnableQuery.class)) continue;
            String fieldName = field.getName();
            String uniqueName = namePrefix.isEmpty() ? fieldName : namePrefix + "_" + fieldName;
            String jsonPath = pathPrefix + "." + fieldName;

            Class<?> type = field.getType();

            if (isLeafType(type)) {
                String sqlType = getSqlType(type);
                schema.put(uniqueName, new ModelField(jsonPath, sqlType));
            }
            else if (Map.class.isAssignableFrom(type)) {
                handleGenericMap(field, uniqueName, jsonPath, schema, depth);
            }
            else if (!Collection.class.isAssignableFrom(type) && !type.isArray()) {
                scanRecursive(type, uniqueName, jsonPath, schema, depth + 1);
            }
        }
    }

    private static void handleGenericMap(Field field, String namePrefix, String pathPrefix, Map<String, ModelField> schema, int depth) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] typeArgs = pt.getActualTypeArguments();

            if (typeArgs.length == 2) {
                Class<?> keyClass = (Class<?>) typeArgs[0];
                Class<?> valueClass = (Class<?>) typeArgs[1];

                if (keyClass.isEnum()) {
                    for (Object enumConstant : keyClass.getEnumConstants()) {
                        String keyName = enumConstant.toString();

                        String nextName = namePrefix + "_" + keyName;
                        String nextPath = pathPrefix + "." + keyName;

                        if (isLeafType(valueClass)) {
                            // *** CHANGE 2: Handle Map Value Type ***
                            String sqlType = getSqlType(valueClass);
                            schema.put(nextName, new ModelField(nextPath, sqlType));
                        } else {
                            scanRecursive(valueClass, nextName, nextPath, schema, depth + 1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Maps Java Types to MySQL Types
     */
    private static String getSqlType(Class<?> t) {
        if (t == int.class || t == Integer.class) return "INT";
        if (t == long.class || t == Long.class) return "BIGINT";
        if (t == double.class || t == Double.class || t == float.class || t == Float.class) return "DOUBLE";
        if (t == boolean.class || t == Boolean.class) return "TINYINT"; // MySQL boolean

        // Default for Strings, UUIDs, Enums
        return "VARCHAR(255)";
    }

    private static boolean shouldIgnore(Field f) {
        int m = f.getModifiers();
        return Modifier.isStatic(m) || Modifier.isTransient(m) || f.isAnnotationPresent(JsonIgnore.class);
    }

    private static boolean isLeafType(Class<?> t) {
        return t.isPrimitive() ||
                Number.class.isAssignableFrom(t) ||
                Boolean.class.isAssignableFrom(t) ||
                String.class.isAssignableFrom(t) ||
                UUID.class.isAssignableFrom(t) ||
                Enum.class.isAssignableFrom(t);
    }
}