package com.itranswarp.bean;

public class LinkBean extends AbstractRequestBean {

	public String name;
	public String url;

	@Override
	public void validate(boolean createMode) {
		this.name = checkName(this.name);
		this.url = checkUrl(this.url);
	}
}
