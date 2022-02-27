package com.itranswarp.bean;

public class SinglePageBean extends AbstractRequestBean {

    public String name;
    public String tags;
    public String content;
    public long publishAt;

    @Override
    public void validate(boolean createMode) {
        this.name = checkName(this.name);
        this.tags = checkTags(this.tags);
        if (createMode || this.content != null) {
            this.content = checkContent(this.content);
        }
        checkTimestamp("publishAt", this.publishAt);
    }

}
