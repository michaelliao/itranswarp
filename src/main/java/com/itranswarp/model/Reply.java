package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "replies", indexes = @Index(name = "IDX_TOPICID", columnList = "topicId"))
public class Reply extends AbstractEntity {

    @Column(nullable = false, updatable = false)
    public long topicId;

    @Column(nullable = false, updatable = false)
    public long userId;

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String userName;

    @Column(nullable = false, length = VAR_CHAR_URL)
    public String userImageUrl;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    public String content;

}
