package net.rainbowcreation.storage.api.utils;

import net.rainbowcreation.storage.api.ModelField;
import net.rainbowcreation.storage.api.annotations.EnableQuery;

import java.lang.reflect.Field;

import java.util.*;

public class SchemaScanner {
    public static Map<String, ModelField> scan(Class<?> clazz) {
        Map<String, ModelField> fields = new HashMap<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (f.isAnnotationPresent(EnableQuery.class)) {
                    String name = f.getName();
                    String sqlType = mapType(f.getType());

                    if (sqlType != null) {
                        fields.put(name, new ModelField("$." + name, sqlType, true));
                    }
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String mapType(Class<?> t) {
        // Explicitly distinguish Boolean vs Numbers
        if (t == boolean.class || t == Boolean.class) return "BOOLEAN";

        // Numbers
        if (t == byte.class || t == Byte.class)       return "TINYINT";
        if (t == int.class || t == Integer.class)     return "INT";
        if (t == long.class || t == Long.class)       return "BIGINT";
        if (t == double.class || t == Double.class)   return "DOUBLE";
        if (t == float.class || t == Float.class)     return "FLOAT";

        // Strings
        if (t == String.class) return "VARCHAR(255)";

        return null;
    }
}