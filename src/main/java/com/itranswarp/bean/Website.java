package com.itranswarp.bean;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public class Website extends AbstractSettingBean {

	@SettingInput(value = InputType.TEXT, description = "Name")
	public String name;

	@SettingInput(value = InputType.TEXT, description = "Description")
	public String description;

	@SettingInput(value = InputType.TEXT, description = "Language code")
	public String lang;

	@SettingInput(value = InputType.TEXT, description = "Keywords")
	public String keywords;

	@SettingInput(value = InputType.TEXT, description = "XML namespace")
	public String xmlns;

	@SettingInput(value = InputType.TEXTAREA, description = "Custom header")
	public String customHeader;

	@SettingInput(value = InputType.TEXTAREA, description = "Custom footer")
	public String customFooter;

}
