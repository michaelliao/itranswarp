package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "boards", uniqueConstraints = @UniqueConstraint(name = "UNI_TAG", columnNames = { "tag" }))
public class Board extends AbstractSortableEntity {

    @Column(nullable = false)
    public long topicNumber;

    @Column(nullable = false)
    public boolean locked;

    @Column(nullable = false, length = VAR_ENUM)
    public String tag;

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false, length = VAR_CHAR_DESCRIPTION)
    public String description;

}
