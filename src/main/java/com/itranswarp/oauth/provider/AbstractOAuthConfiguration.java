package com.itranswarp.oauth.provider;

public class AbstractOAuthConfiguration {

    private String name;

    private String icon;

    private String color;

    private String clientId;

    private String clientSecret;

    private boolean ignoreImage;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isIgnoreImage() {
        return ignoreImage;
    }

    public void setIgnoreImage(boolean ignoreImage) {
        this.ignoreImage = ignoreImage;
    }

}
