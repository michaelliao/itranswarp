package com.itranswarp.bean;

public class WikiPageBean extends AbstractRequestBean {

    public long parentId;
    public String name;
    public String content;
    public long publishAt;

    @Override
    public void validate(boolean createMode) {
        if (createMode) {
            checkId("parentId", this.parentId);
        }
        this.name = checkName(this.name);
        if (createMode || this.content != null) {
            this.content = checkContent(this.content);
        }
        checkTimestamp("publishAt", this.publishAt);
    }

}
