package com.itranswarp.bean;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public class Website extends AbstractSettingBean {

	@SettingInput(value = InputType.TEXT, order = 1, description = "Name")
	public String name;

	@SettingInput(value = InputType.TEXT, order = 2, description = "Description")
	public String description;

	@SettingInput(value = InputType.TEXT, order = 3, description = "Language code")
	public String lang;

	@SettingInput(value = InputType.TEXT, order = 4, description = "Keywords")
	public String keywords;

	@SettingInput(value = InputType.TEXT, order = 5, description = "XML namespace")
	public String xmlns;

	@SettingInput(value = InputType.TEXTAREA, order = 6, description = "Custom header")
	public String customHeader;

	@SettingInput(value = InputType.TEXTAREA, order = 7, description = "Custom footer")
	public String customFooter;

}
