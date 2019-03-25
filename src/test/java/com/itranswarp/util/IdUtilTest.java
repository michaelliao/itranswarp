package com.itranswarp.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IdUtilTest {

	@Test
	void testNextId() {
		for (int i = 0; i < 40; i++) {
			long id = IdUtil.nextId(); 
			System.out.println(id);
		}
	}

	@Test
	void testLongIdToShortId() {
		assertEquals(1791764829243392L, IdUtil.stringIdToLongId("0013738748248885ddf38d8cd1b4803aa74bcda32f853fd000"));
		assertEquals(1791769413708832L, IdUtil.stringIdToLongId("001373875917148f989cdeb2b27441d95112edb37834a0b000"));
		assertEquals(1939911498267008L, IdUtil.stringIdToLongId("001409195742008d822b26cf3de46aea14f2b7378a1ba91000"));
		assertEquals(2046041490717248L, IdUtil.stringIdToLongId("0014344991049250a2c80ec84cb4861bbd1d9b2c0c2850e000"));
		assertEquals(2517661497948480L, IdUtil.stringIdToLongId("001546942076610f8e8bc7218ed4a7eb597ed2658047a17000"));
		assertEquals(2541567235588064L, IdUtil.stringIdToLongId("0015526416484437ca3e4b736e54075a9d5fec66c691a6e000"));
	}

}
