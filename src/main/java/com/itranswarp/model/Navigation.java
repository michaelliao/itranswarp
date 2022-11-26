package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "navigations")
public class Navigation extends AbstractSortableEntity {

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false, length = VAR_ENUM)
    public String icon;

    @Column(nullable = false, length = VAR_CHAR_URL)
    public String url;

    /**
     * Add "target=_blank" for this link.
     */
    @Column(nullable = false)
    public boolean blank;

}
