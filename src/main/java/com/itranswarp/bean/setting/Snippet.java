package com.itranswarp.bean.setting;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public class Snippet extends AbstractSettingBean {

    @SettingInput(value = InputType.TEXTAREA, order = 1, description = "Index top")
    public String indexTop;

    @SettingInput(value = InputType.TEXTAREA, order = 2, description = "Index bottom")
    public String indexBottom;

    @SettingInput(value = InputType.TEXTAREA, order = 3, description = "Body top")
    public String bodyTop;

    @SettingInput(value = InputType.TEXTAREA, order = 4, description = "Body bottom")
    public String bodyBottom;

    @SettingInput(value = InputType.TEXTAREA, order = 5, description = "Content top")
    public String contentTop;

    @SettingInput(value = InputType.TEXTAREA, order = 6, description = "Content bottom")
    public String contentBottom;

    @SettingInput(value = InputType.TEXTAREA, order = 7, description = "Left side bar top")
    public String sidebarLeftTop;

    @SettingInput(value = InputType.TEXTAREA, order = 8, description = "Left side bar bottom")
    public String sidebarLeftBottom;

    @SettingInput(value = InputType.TEXTAREA, order = 9, description = "Right side bar top")
    public String sidebarRightTop;

    @SettingInput(value = InputType.TEXTAREA, order = 10, description = "Right side bar bottom")
    public String sidebarRightBottom;

}
