package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "settings", uniqueConstraints = @UniqueConstraint(name = "UNI_GRP_KEY", columnNames = { "settingGroup", "settingKey" }))
public class Setting extends AbstractEntity {

    public static final String GROUP_WEBSITE = "Website";
    public static final String GROUP_SNIPPETS = "Snippets";

    @Column(nullable = false, length = VAR_ENUM)
    public String settingGroup;

    @Column(nullable = false, length = VAR_ENUM)
    public String settingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String settingValue;

}
