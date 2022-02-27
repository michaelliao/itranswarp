package com.itranswarp.bean;

public class ArticleBean extends AbstractRequestBean {

    public long categoryId;

    public String name;

    public String tags;

    public String description;

    public String content;

    public String image;

    public long publishAt;

    @Override
    public void validate(boolean createMode) {
        checkId("categoryId", categoryId);
        this.name = checkName(this.name);
        this.tags = checkTags(this.tags);
        this.description = checkDescription(this.description);
        if (createMode || this.content != null) {
            this.content = checkContent(this.content);
        }
        if (createMode || this.image != null) {
            this.image = checkImage(this.image);
        }
        checkTimestamp("publishAt", this.publishAt);
    }

}
