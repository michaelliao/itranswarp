package com.itranswarp.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AntiSpamServiceTest {

	AntiSpamService antiSpamService;

	@BeforeEach
	void setUp() throws Exception {
		this.antiSpamService = new AntiSpamService();
	}

	@Test
	void testIsSpam() {
		this.antiSpamService.setSpamKeywords(List.of("123456", "world"));
		assertTrue(this.antiSpamService.isSpam("add QQ: ①②③④⑤⑥"));
		assertTrue(this.antiSpamService.isSpam("Hello, spam world!"));
		assertFalse(this.antiSpamService.isSpam("add QQ: ①②③④⑤"));
		assertFalse(this.antiSpamService.isSpam("qq: 123 456"));
	}

	@Test
	void testNormalize() {
		assertEquals("qq:123456789", this.antiSpamService.normalize("QQ：①②③④⑤⑥⑦⑧⑨"));
		assertEquals("qq:123456789", this.antiSpamService.normalize("QQ：㊀㊁㊂㊃㊄㊅㊆㊇㊈"));
		assertEquals("qq tel end", this.antiSpamService.normalize("  QQ　　　TEL   END  "));
	}
}
