package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
