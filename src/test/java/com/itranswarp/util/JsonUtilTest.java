package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

public class JsonUtilTest {

    public static class Person {

        public String name;
        public int age;
        public BigDecimal salary;

        public Person() {
        }

        public Person(String name, int age, BigDecimal salary) {
            this.name = name;
            this.age = age;
            this.salary = salary;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Person) {
                Person p = (Person) o;
                return Objects.equals(p.name, this.name) && p.age == this.age && Objects.equals(p.salary, this.salary);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.age, this.salary);
        }
    }

    static final BigDecimal S10 = new BigDecimal("10.12001");
    static final BigDecimal S99 = new BigDecimal("99.10001");

    @Test
    public void testJsonObject() {
        Person p = new Person("Bob", 19, S10);
        assertEquals(p, JsonUtil.readJson(JsonUtil.writeJson(p), Person.class));
        assertEquals(p, JsonUtil.readJson(JsonUtil.writeJsonAsBytes(p), Person.class));
    }

    @Test
    public void testJsonString() {
        assertEquals("\"Bob\"", JsonUtil.writeJson("Bob"));
        assertEquals("\"Bob'N\\u0000N\"", JsonUtil.writeJson("Bob'N\u0000N"));
    }

    @Test
    public void testBigDecimal() {
        final BigDecimal big = new BigDecimal("10.12000000000000010001");
        Number[] array = new Number[] { big };
        String str = "[10.12000000000000010001]";
        assertEquals(str, JsonUtil.writeJson(array));
        assertArrayEquals(str.getBytes(StandardCharsets.UTF_8), JsonUtil.writeJsonAsBytes(array));
        Number[] read = JsonUtil.readJson(str, BigDecimal[].class);
        assertEquals(1, read.length);
        // strict equal:
        assertEquals(big, read[0]);
    }

    @Test
    public void testMapBigDecimal() {
        final BigDecimal big = new BigDecimal("10.12000000000000010001");
        final Map<String, TestBean> map = new HashMap<>();
        final String json = "{\"t\":{\"value\":10.12000000000000010001}}";
        TestBean t = new TestBean();
        t.value = big;
        map.put("t", t);
        assertEquals(json, JsonUtil.writeJson(map));
        // deserialize:
        Map<String, TestBean> r = JsonUtil.readJson(json, new TypeReference<Map<String, TestBean>>() {
        });
        assertNotNull(r);
        assertNotNull(r.get("t"));
        assertEquals(0, big.compareTo(r.get("t").value));
    }

    @Test
    public void testBigLong() {
        for (long i = 0; i < 100; i++) {
            Long n = Long.MAX_VALUE - i;
            String s = JsonUtil.writeJson(n);
            Long parsed = JsonUtil.readJson(s, Long.class);
            assertEquals(n, parsed);
        }
    }

    public static class TestBean {
        public BigDecimal value;
    }
}
