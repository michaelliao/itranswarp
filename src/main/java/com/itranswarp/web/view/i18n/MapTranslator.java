package com.itranswarp.web.view.i18n;

import java.util.HashMap;
import java.util.Map;

/**
 * Map translator.
 * 
 * @author liaoxuefeng
 */
class MapTranslator implements Translator {

    final String displayName;
    final String localeName;
    final Map<String, String> translator;

    MapTranslator(String localeName, String displayName, Map<String, String> map) {
        this.localeName = localeName;
        this.displayName = displayName;
        this.translator = new HashMap<>(map);
    }

    @Override
    public String translate(String text, Object... args) {
        String t = this.translator.getOrDefault(text, text);
        return args.length == 0 ? t : String.format(t, args);
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String getLocaleName() {
        return this.localeName;
    }

}
