package com.itranswarp.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.util.IdUtil;

public class AntiSpamServiceTest {

    AntiSpamService antiSpamService;

    @BeforeAll
    static void initIdUtil() throws ReflectiveOperationException {
        Field f = IdUtil.class.getDeclaredField("shardingId");
        f.setAccessible(true);
        f.set(null, 1L);
    }

    @BeforeEach
    void setUp() throws Exception {
        this.antiSpamService = new AntiSpamService();
    }

    @Test
    void testIsSpam() {
        this.antiSpamService.spamKeywords = List.of("123456", "world");
        assertTrue(this.antiSpamService.isSpamText("add QQ: ①②③④⑤⑥"));
        assertTrue(this.antiSpamService.isSpamText("Hello, spam world!"));
        assertFalse(this.antiSpamService.isSpamText("add QQ: ①②③④⑤"));
        assertFalse(this.antiSpamService.isSpamText("qq: 123 456"));
    }

    @Test
    void testNormalize() {
        assertEquals("qq:123456789", this.antiSpamService.normalize("QQ：①②③④⑤⑥⑦⑧⑨"));
        assertEquals("qq:123456789", this.antiSpamService.normalize("QQ：㊀㊁㊂㊃㊄㊅㊆㊇㊈"));
        assertEquals("qq tel end", this.antiSpamService.normalize("  QQ　　　TEL   END  "));
    }
}
