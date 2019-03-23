package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IdUtilTest {

	@Test
	void testNextId() {
		for (int i = 0; i < 40; i++) {
			String id = IdUtil.nextId();
			assertEquals(12, id.length());
			System.out.println(id);
		}
	}

	@Test
	void testLongIdToShortId() {
		assertEquals("0nazye647da0", IdUtil.longIdToShortId("001409195742008d822b26cf3de46aea14f2b7378a1ba91000"));
		assertEquals("0poehyo950a0", IdUtil.longIdToShortId("0015526416484437ca3e4b736e54075a9d5fec66c691a6e000"));
		assertEquals("0nq2alc24ed0", IdUtil.longIdToShortId("0014344991049250a2c80ec84cb4861bbd1d9b2c0c2850e000"));
		assertEquals("0mpywlk719d0", IdUtil.longIdToShortId("0013738748248885ddf38d8cd1b4803aa74bcda32f853fd000"));
		assertEquals("0pl0c58ca1b0", IdUtil.longIdToShortId("001546942076610f8e8bc7218ed4a7eb597ed2658047a17000"));
		assertEquals("0mpyxfx33060", IdUtil.longIdToShortId("001373875917148f989cdeb2b27441d95112edb37834a0b000"));
	}

}
