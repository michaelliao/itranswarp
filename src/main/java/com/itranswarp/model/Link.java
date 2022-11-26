package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "links")
public class Link extends AbstractEntity {

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false, length = VAR_CHAR_URL)
    public String url;

    @Transient
    public String getShortenUrl() {
        return "/link/" + this.id;
    }
}
