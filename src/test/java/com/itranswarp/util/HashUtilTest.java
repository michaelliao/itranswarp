package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class HashUtilTest {

    @Test
    public void testSha1() {
        assertEquals("1f09d30c707d53f3d16c530dd73d70a6ce7596a9", HashUtil.sha1("hello, world!".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testSha256() {
        assertEquals("68e656b251e67e8358bef8483ab0d51c6619f3e7a1a9f0e75838d41ff368f728", HashUtil.sha256("hello, world!".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testHmacSha256() {
        byte[] message = "hello, world!".getBytes(StandardCharsets.UTF_8);
        assertEquals("891b3504c17d305ca7eda26cd4582664288113d48ec96a9cbb4d529ab716f0ce", HashUtil.hmacSha256(message, "secret"));
    }
}
