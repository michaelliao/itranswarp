package com.itranswarp.web.view.i18n;

public interface Translator {

    String getDisplayName();

    String getLocaleName();

    String translate(String text, Object... args);

}
