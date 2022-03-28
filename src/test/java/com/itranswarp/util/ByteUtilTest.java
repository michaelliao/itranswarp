package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ByteUtilTest {

    @Test
    public void testToHexString() {
        assertEquals("010203fffefd", ByteUtil.toHexString(new byte[] { 1, 2, 3, -1, -2, -3 }));
    }

    @Test
    public void testHexString() {
        byte[] message = new byte[] { -128, -127, -126, -125, -124, -4 - 3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 124, 125, 126, 127 };
        String hex = ByteUtil.toHexString(message);
        assertEquals("8081828384f9feff000102030405067c7d7e7f", hex);
        byte[] data = ByteUtil.fromHexString(hex);
        assertArrayEquals(message, data);
    }

    @Test
    public void testFromHex() {
        assertEquals((byte) 0, ByteUtil.fromHex("00"));
        assertEquals((byte) 1, ByteUtil.fromHex("01"));
        assertEquals((byte) 15, ByteUtil.fromHex("0f"));
        assertEquals((byte) -1, ByteUtil.fromHex("ff"));
        assertEquals((byte) -2, ByteUtil.fromHex("fe"));
        assertEquals((byte) -16, ByteUtil.fromHex("f0"));
        assertEquals((byte) -127, ByteUtil.fromHex("81"));
        assertEquals((byte) -128, ByteUtil.fromHex("80"));
    }

    @Test
    public void testToHex() {
        assertEquals("00", ByteUtil.toHex((byte) 0));
        assertEquals("01", ByteUtil.toHex((byte) 1));
        assertEquals("0f", ByteUtil.toHex((byte) 15));
        assertEquals("ff", ByteUtil.toHex((byte) -1));
        assertEquals("fe", ByteUtil.toHex((byte) -2));
        assertEquals("f0", ByteUtil.toHex((byte) -16));
        assertEquals("81", ByteUtil.toHex((byte) -127));
        assertEquals("80", ByteUtil.toHex((byte) -128));
    }
}
