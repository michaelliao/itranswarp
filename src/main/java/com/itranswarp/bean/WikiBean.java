package com.itranswarp.bean;

public class WikiBean extends AbstractRequestBean {

    public String name;
    public String tag;
    public String description;

    /**
     * Markdown content.
     */
    public String content;

    /**
     * Base64 encoded image data.
     */
    public String image;

    public long publishAt;

    @Override
    public void validate(boolean createMode) {
        this.name = checkName(this.name);
        this.tag = checkTag(this.tag);
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
