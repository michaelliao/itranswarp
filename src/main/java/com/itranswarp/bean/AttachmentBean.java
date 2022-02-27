package com.itranswarp.bean;

public class AttachmentBean extends AbstractRequestBean {

    public String name;
    public String data;

    @Override
    public void validate(boolean createMode) {
        this.name = checkName(this.name);
        this.data = checkImage(this.data);
    }

}
