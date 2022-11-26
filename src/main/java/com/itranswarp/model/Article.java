package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "articles", indexes = @Index(name = "IDX_CAT_PUB", columnList = "categoryId,publishAt"))
public class Article extends AbstractEntity {

    @Column(nullable = false, updatable = false)
    public long userId;

    @Column(nullable = false)
    public long categoryId;

    /**
     * Reference to attachment id.
     */
    @Column(nullable = false)
    public long imageId;

    @Column(nullable = false)
    public long textId;

    @Column(nullable = false)
    public long views;

    @Column(nullable = false, length = VAR_CHAR_TAGS)
    public String tags;

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false, length = VAR_CHAR_DESCRIPTION)
    public String description;

    @Column(nullable = false)
    public long publishAt;

}
