package com.itranswarp.bean;

public class WikiPageBean extends AbstractRequestBean {

	public String name;
	public String content;
	public long publishAt;

	@Override
	public void validate(boolean createMode) {
		this.name = checkName(this.name);
		if (createMode || this.content != null) {
			this.content = checkContent(this.content);
		}
		checkPublishAt(this.publishAt);
	}

}
