package com.itranswarp.bean.setting;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public class SettingDefinition {

    public final String name;
    public final String description;
    public final InputType type;

    public SettingDefinition(String name, SettingInput input) {
        this.name = name;
        this.description = input.description();
        this.type = input.value();
    }
}
