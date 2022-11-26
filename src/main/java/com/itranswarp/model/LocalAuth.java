package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Store local users with password authentication.
 * 
 * @author liaoxuefeng
 */
@Entity
@Table(name = "local_auths", uniqueConstraints = @UniqueConstraint(name = "UNI_UID", columnNames = { "userId" }))
public class LocalAuth extends AbstractEntity {

    @Column(nullable = false, updatable = false)
    public long userId;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_HASH)
    public String passwd;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_HASH)
    public String salt;

}
