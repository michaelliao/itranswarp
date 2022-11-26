package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "single_pages")
public class SinglePage extends AbstractEntity {

    @Column(nullable = false)
    public long textId;

    @Column(nullable = false, length = VAR_CHAR_TAGS)
    public String tags;

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false)
    public long publishAt;

}
