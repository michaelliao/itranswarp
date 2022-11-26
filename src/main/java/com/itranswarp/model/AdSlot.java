package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ad_slots")
public class AdSlot extends AbstractEntity {

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public String alias;

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false, length = VAR_CHAR_DESCRIPTION)
    public String description;

    @Column(nullable = false)
    public long price;

    @Column(nullable = false, updatable = false)
    public long width;

    @Column(nullable = false, updatable = false)
    public long height;

    @Column(nullable = false)
    public long numSlots;

    @Column(nullable = false)
    public long numAutoFill;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String adAutoFill;

}
