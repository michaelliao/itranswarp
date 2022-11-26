package com.itranswarp.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.itranswarp.bean.setting.Security;
import com.itranswarp.common.AbstractService;

@Component
public class AntiSpamService extends AbstractService {

    @Autowired
    SettingService settingService;

    List<String> spamKeywords = List.of();
    Set<String> spamIps = Set.of();

    private final Map<Character, Character> charMap = createMapping();

    private Map<Character, Character> createMapping() {
        CharMapping[] mappings = new CharMapping[] { //
                new CharMapping("\u3000", ' '), // space
                new CharMapping("０零", '0'), //
                new CharMapping("１⒈①⑴㊀㈠一壹", '1'), //
                new CharMapping("２⒉②⑵㊁㈡二贰", '2'), //
                new CharMapping("３⒊③⑶㊂㈢三叁", '3'), //
                new CharMapping("４⒋④⑷㊃㈣四肆", '4'), //
                new CharMapping("５⒌⑤⑸㊄㈤五伍", '5'), //
                new CharMapping("６⒍⑥⑹㊅㈥六陆", '6'), //
                new CharMapping("７⒎⑦⑺㊆㈦七柒", '7'), //
                new CharMapping("８⒏⑧⑻㊇㈧八捌", '8'), //
                new CharMapping("９⒐⑨⑼㊈㈨九玖", '9'), //
                new CharMapping("【〖", '['), //
                new CharMapping("】〗", ']'), //
                new CharMapping("。", '.'), //
                new CharMapping("，", ','), //
                new CharMapping("；", ';'), //
                new CharMapping("：", ':'), //
                new CharMapping("！", '!'), //
                new CharMapping("？", '?'), //
                new CharMapping("（", '('), //
                new CharMapping("）", ')') };
        Map<Character, Character> map = new HashMap<>();
        for (CharMapping mapping : mappings) {
            for (int i = 0; i < mapping.from.length(); i++) {
                map.put(mapping.from.charAt(i), mapping.to);
            }
        }
        return map;
    }

    @PostConstruct
    public void init() {
        scheduledReload();
    }

    public boolean isSpamIp(String ip) {
        return this.spamIps.contains(ip);
    }

    public boolean isSpamText(String text) {
        if (this.spamKeywords.isEmpty()) {
            return false;
        }
        String normalized = normalize(text);
        for (String spam : this.spamKeywords) {
            if (normalized.contains(spam)) {
                logger.warn("spam detected by keyword {}: {}", spam, text);
                return true;
            }
        }
        return false;
    }

    @Scheduled(cron = "0 0/10 * * * *")
    public void scheduledReload() {
        logger.info("scheduled reload spam ips and keywords...");
        Security security = settingService.getSecurity();
        this.spamKeywords = security.getSpamKeywordsAsList();
        this.spamIps = security.getIpBlacklistAsSet();
    }

    String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean lastIsSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            Character c = charMap.get(ch);
            char convert = c == null ? Character.toLowerCase(ch) : c.charValue();
            boolean isSpace = Character.isWhitespace(convert);
            if (!lastIsSpace || !isSpace) {
                sb.append(isSpace ? ' ' : convert);
            }
            lastIsSpace = isSpace;
        }
        return sb.toString().trim();
    }
}

class CharMapping {
    String from;
    char to;

    CharMapping(String from, char to) {
        this.from = from;
        this.to = to;
    }
}