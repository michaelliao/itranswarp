package com.itranswarp;

import java.time.Instant;
import java.time.ZoneId;

public class Test {

	public static void main(String[]args)throws Exception{
		long ts=0b11111_11111111_11111111_11111111_111L;
		System.out.println(ts+" "+Instant.ofEpochSecond(ts).atZone(ZoneId.of("Z")).toString());
		System.out.println(0b111111_11111111_11L);
	}
}
