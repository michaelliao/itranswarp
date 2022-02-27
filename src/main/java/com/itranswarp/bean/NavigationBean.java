package com.itranswarp.bean;

public class NavigationBean extends AbstractRequestBean {

    public String name;
    public String icon;
    public String url;
    public boolean blank;

    @Override
    public void validate(boolean createMode) {
        this.name = checkName(this.name);
        this.icon = checkIcon(this.icon);
        this.url = checkUrl(this.url);
    }

}
