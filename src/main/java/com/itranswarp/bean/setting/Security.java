package com.itranswarp.bean.setting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public class Security extends AbstractSettingBean {

    @SettingInput(value = InputType.TEXTAREA, order = 1, description = "Forbidden IPs")
    public String ipBlacklist;

    @SettingInput(value = InputType.TEXTAREA, order = 2, description = "Spam keywords")
    public String spamKeywords;

    public Set<String> getIpBlacklistAsSet() {
        return new HashSet<>(splitByLines(this.ipBlacklist, false));
    }

    public List<String> getSpamKeywordsAsList() {
        return splitByLines(this.spamKeywords, true);
    }

    private List<String> splitByLines(String text, boolean toLowerCase) {
        if (text == null) {
            text = "";
        }
        if (toLowerCase) {
            text = text.toLowerCase();
        }
        return Arrays.stream(text.split("\n")).map(s -> s.strip()).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList());
    }
}
