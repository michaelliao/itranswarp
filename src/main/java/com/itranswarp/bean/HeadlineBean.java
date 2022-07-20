package com.itranswarp.bean;

public class HeadlineBean extends AbstractRequestBean {

    public String name;

    public String description;

    public String url;

    @Override
    public void validate(boolean createMode) {
        this.name = checkName(this.name);
        this.description = checkDescription(this.description);
        this.url = checkUrl(this.url, true);
    }

}
