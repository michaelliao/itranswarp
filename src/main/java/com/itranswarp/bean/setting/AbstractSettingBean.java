package com.itranswarp.bean.setting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public abstract class AbstractSettingBean {

    @JsonIgnore
    public Map<String, String> getSettings() {
        return Arrays.stream(getPublicStringFields()).collect(Collectors.toMap(f -> f.getName(), f -> {
            try {
                String value = (String) f.get(this);
                return value == null ? "" : value;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void setSettings(Map<String, String> map) {
        Arrays.stream(getPublicStringFields()).forEach(f -> {
            String value = map.get(f.getName());
            if (value == null) {
                value = "";
            }
            try {
                f.set(this, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Map<String, List<SettingDefinition>> settingDefinitions = new ConcurrentHashMap<>();

    @JsonIgnore
    public List<SettingDefinition> getSettingDefinitions() {
        String key = getClass().getName();
        List<SettingDefinition> defs = settingDefinitions.get(key);
        if (defs == null) {
            defs = Arrays.stream(getPublicStringFields()).sorted((f1, f2) -> {
                SettingInput s1 = f1.getAnnotation(SettingInput.class);
                SettingInput s2 = f2.getAnnotation(SettingInput.class);
                int n = Integer.compare(s1.order(), s2.order());
                if (n == 0) {
                    return f1.getName().compareTo(f2.getName());
                }
                return n;
            }).map(field -> new SettingDefinition(field.getName(), field.getAnnotation(SettingInput.class))).collect(Collectors.toList());
            settingDefinitions.put(key, defs);
        }
        return defs;
    }

    public InputType getInputType(String name) {
        try {
            Field field = getClass().getField(name);
            SettingInput input = field.getAnnotation(SettingInput.class);
            if (input != null) {
                return input.value();
            }
            return InputType.TEXT;
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException("No such field " + name, e);
        }
    }

    private Field[] getPublicStringFields() {
        Field[] fields = getClass().getFields();
        return Arrays.stream(fields).filter(f -> f.getType() == String.class // only for String
                && Modifier.isPublic(f.getModifiers()) // public
                && !Modifier.isStatic(f.getModifiers()) // non-static
                && !Modifier.isFinal(f.getModifiers()) // non-final
        ).map(f -> {
            f.setAccessible(true);
            return f;
        }).toArray(Field[]::new);
    }
}
