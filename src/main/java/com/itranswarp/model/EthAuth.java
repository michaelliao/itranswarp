package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "eth_auths", uniqueConstraints = @UniqueConstraint(name = "UNI_ADDR", columnNames = { "address" }))
public class EthAuth extends AbstractEntity {

    @Column(nullable = false, updatable = false)
    public long userId;

    @Column(nullable = false, updatable = false, length = 42)
    public String address;

    /**
     * Eth signature expires time.
     */
    @Column(nullable = false)
    public long expiresAt;

}
