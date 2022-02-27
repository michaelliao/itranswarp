package com.itranswarp.bean;

public class DownloadBean {

    public final String mime;
    public final byte[] data;

    public DownloadBean(String mime, byte[] data) {
        this.mime = mime;
        this.data = data;
    }

}
