package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Store attachment which links resources with article, wiki or wikipages.
 * 
 * @author liaoxuefeng
 */
@Entity
@Table(name = "attachments")
public class Attachment extends AbstractEntity {

    @Column(nullable = false, updatable = false)
    public long userId;

    @Column(nullable = false, updatable = false)
    public long resourceId;

    @Column(nullable = false, updatable = false)
    public long size;

    @Column(nullable = false, updatable = false)
    public int width;

    @Column(nullable = false, updatable = false)
    public int height;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_MIME)
    public String mime;

}
