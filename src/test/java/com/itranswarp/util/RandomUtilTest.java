package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class RandomUtilTest {

    @Test
    public void testRandomString() {
        for (int i = 0; i < 10; i++) {
            String s = RandomUtil.createRandomString("abcdefg", 10);
            System.out.println(s);
            assertTrue(s.matches("[a-g]{10}"));
        }
        for (int i = 0; i < 10; i++) {
            String s = RandomUtil.createRandomString("abcdefABCDEF", 20);
            System.out.println(s);
            assertTrue(s.matches("[a-fA-F]{20}"));
        }
        for (int i = 0; i < 10; i++) {
            String s = RandomUtil.createRandomString(RandomUtil.WORDS, 30);
            System.out.println(s);
            assertTrue(s.matches("[a-zA-Z0-9]{30}"));
        }
    }

}
