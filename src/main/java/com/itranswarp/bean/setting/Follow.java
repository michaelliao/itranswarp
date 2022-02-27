package com.itranswarp.bean.setting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public class Follow extends AbstractSettingBean {

    @SettingInput(value = InputType.TEXT, order = 1, description = "Facebook URL")
    public String facebook;

    @SettingInput(value = InputType.TEXT, order = 2, description = "Github URL")
    public String github;

    @SettingInput(value = InputType.TEXT, order = 3, description = "Instagram URL")
    public String instagram;

    @SettingInput(value = InputType.TEXT, order = 3, description = "Linkedin URL")
    public String linkedin;

    @SettingInput(value = InputType.TEXT, order = 4, description = "Reddit URL")
    public String reddit;

    @SettingInput(value = InputType.TEXT, order = 5, description = "Twitter URL")
    public String twitter;

    @SettingInput(value = InputType.TEXT, order = 7, description = "Weibo URL")
    public String weibo;

    @SettingInput(value = InputType.TEXT, order = 8, description = "Youtube URL")
    public String youtube;

    public List<String[]> getFollows() {
        List<String[]> list = new ArrayList<>();
        FIELDS.forEach(f -> {
            String value = null;
            try {
                value = (String) f.get(this);
            } catch (IllegalAccessException e) {
            }
            if (value != null && !value.isEmpty()) {
                list.add(new String[] { f.getName(), value });
            }
        });
        return list;
    };

    private static final List<Field> FIELDS = Arrays.stream(Follow.class.getFields()).sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
            .collect(Collectors.toList());
}
