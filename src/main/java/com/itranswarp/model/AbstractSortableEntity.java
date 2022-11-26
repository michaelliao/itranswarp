package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractSortableEntity extends AbstractEntity {

    @Column(nullable = false)
    public long displayOrder;

}
