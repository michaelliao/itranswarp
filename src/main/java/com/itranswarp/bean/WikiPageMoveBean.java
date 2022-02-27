package com.itranswarp.bean;

public class WikiPageMoveBean extends AbstractRequestBean {

    public long parentId;
    public int displayIndex;

    @Override
    public void validate(boolean createMode) {
        checkId("parentId", parentId);
    }

}
