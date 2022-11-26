package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IdUtilTest {

    @BeforeAll
    static void initIdUtil() throws ReflectiveOperationException {
        Field f = IdUtil.class.getDeclaredField("shardingId");
        f.setAccessible(true);
        f.set(null, 1L);
    }

    @Test
    void testNextId() {
        for (int i = 0; i < 40; i++) {
            long id = IdUtil.nextId();
            System.out.println(id);
        }
    }

    @Test
    void test64KIdIn1Sec() {
        for (int i = 0; i < 65534; i++) {
            IdUtil.nextId();
        }
        // nextId: 1111111111111111xxxxx:
        assertEquals(0b1111111111111111_00000L, IdUtil.nextId() & 0b1111111111111111_00000L);
        assertEquals(0b01_00000L, IdUtil.nextId() & 0b1111111111111111_00000L);
        assertEquals(0b10_00000L, IdUtil.nextId() & 0b1111111111111111_00000L);
    }

    @Test
    void testLongIdToShortId() {
        assertEquals(895882413934592L, IdUtil.stringIdToLongId("0013738748248885ddf38d8cd1b4803aa74bcda32f853fd000"));
        assertEquals(895884706212896L, IdUtil.stringIdToLongId("001373875917148f989cdeb2b27441d95112edb37834a0b000"));
        assertEquals(969955749132672L, IdUtil.stringIdToLongId("001409195742008d822b26cf3de46aea14f2b7378a1ba91000"));
        assertEquals(1023020745357888L, IdUtil.stringIdToLongId("0014344991049250a2c80ec84cb4861bbd1d9b2c0c2850e000"));
        assertEquals(1258830748973376L, IdUtil.stringIdToLongId("001546942076610f8e8bc7218ed4a7eb597ed2658047a17000"));
        assertEquals(1270783617794016L, IdUtil.stringIdToLongId("0015526416484437ca3e4b736e54075a9d5fec66c691a6e000"));
    }
}
