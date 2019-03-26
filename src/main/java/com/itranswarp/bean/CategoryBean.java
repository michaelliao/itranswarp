package com.itranswarp.bean;

public class CategoryBean extends AbstractRequestBean {

	public String name;
	public String description;

	@Override
	public void validate(boolean createMode) {
		this.name = checkName(this.name);
		this.description = checkDescription(this.description);
	}

}
