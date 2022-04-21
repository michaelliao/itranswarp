package com.itranswarp.search;

public class SearchableDocument {

    public static final String TYPE_ARTICLE = "article";
    public static final String TYPE_WIKI = "wiki";
    public static final String TYPE_WIKI_PAGE = "wikipage";

    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_PUBLISH_AT = "publishAt";

    public long id;

    public String type;

    public String name;

    public String content;

    public long publishAt;

    public String url;
}
