package com.itranswarp.bean;

import com.itranswarp.anno.SettingInput;
import com.itranswarp.enums.InputType;

public class Snippet extends AbstractSettingBean {

	@SettingInput(value = InputType.TEXTAREA, description = "Index top")
	public String indexTop;

	@SettingInput(value = InputType.TEXTAREA, description = "Index bottom")
	public String indexBottom;

	@SettingInput(value = InputType.TEXTAREA, description = "Body top")
	public String bodyTop;

	@SettingInput(value = InputType.TEXTAREA, description = "Body bottom")
	public String bodyBottom;

	@SettingInput(value = InputType.TEXTAREA, description = "Left side bar top")
	public String leftSideBarTop;

	@SettingInput(value = InputType.TEXTAREA, description = "Left side bar bottom")
	public String leftSideBarBottom;

	@SettingInput(value = InputType.TEXTAREA, description = "Right side bar top")
	public String rightSideBarTop;

	@SettingInput(value = InputType.TEXTAREA, description = "Right side bar bottom")
	public String rightSideBarBottom;

	@SettingInput(value = InputType.TEXTAREA, description = "Content top")
	public String contentTop;

	@SettingInput(value = InputType.TEXTAREA, description = "Content bottom")
	public String contentBottom;

}
