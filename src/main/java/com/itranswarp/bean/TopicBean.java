package com.itranswarp.bean;

import com.itranswarp.enums.RefType;

public class TopicBean extends AbstractRequestBean {

    public RefType refType;
    public long refId;
    public String name;
    public String content;

    @Override
    public void validate(boolean createMode) {
        this.name = checkName(this.name);
        this.content = checkContent(this.content);
        if (refType == RefType.NONE) {
            this.refId = 0L;
        } else {
            checkId("refId", this.refId);
        }
    }

}
