package com.itranswarp.bean;

public class BoardBean extends AbstractRequestBean {

    public String name;
    public String tag;
    public String description;

    @Override
    public void validate(boolean createMode) {
        this.name = checkName(this.name);
        this.tag = checkTags(this.tag);
        this.description = checkDescription(this.description);
    }

}
