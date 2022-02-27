package com.itranswarp.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple java bean using public field to copy.
 */
public abstract class AbstractBean {

    public void copyPropertiesTo(Object target) {
        try {
            copyProperties(this, target);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyPropertiesFrom(Object source) {
        try {
            copyProperties(source, this);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyProperties(Object source, Object target) throws IllegalArgumentException, IllegalAccessException {
        Map<String, Field> sourceProps = getProperties(source.getClass());
        Map<String, Field> targetProps = getProperties(target.getClass());
        for (String name : sourceProps.keySet()) {
            Field targetField = targetProps.get(name);
            if (targetField != null) {
                Field sourceField = sourceProps.get(name);
                Object value = sourceField.get(source);
                targetField.set(target, value);
            }
        }
    }

    private static Map<String, Field> getProperties(Class<?> clazz) {
        String key = clazz.getName();
        Map<String, Field> result = cache.get(key);
        if (result == null) {
            result = new HashMap<>();
            for (Field f : clazz.getFields()) {
                int mod = f.getModifiers();
                if (!Modifier.isFinal(mod) && !Modifier.isStatic(mod)) {
                    result.put(f.getName(), f);
                }
            }
            cache.put(key, result);
        }
        return result;
    }

    private static Map<String, Map<String, Field>> cache = new ConcurrentHashMap<>();

}
