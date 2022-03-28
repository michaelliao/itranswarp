package com.itranswarp.search;

public class SearchResult {

    public final String name;
    public final String type;
    public final String url;
    public final String content;

    public SearchResult(String name, String type, String url, String content) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.content = content;
    }
}
