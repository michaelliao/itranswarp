package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "texts", uniqueConstraints = @UniqueConstraint(name = "UNI_HASH", columnNames = { "hash" }))
public class Text extends AbstractEntity {

    @Column(nullable = false, updatable = false, length = VAR_CHAR_HASH)
    public String hash;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    public String content;

}
