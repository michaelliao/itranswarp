package com.itranswarp.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public abstract class AbstractSettingBean {

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
