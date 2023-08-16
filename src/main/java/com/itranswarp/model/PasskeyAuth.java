package com.itranswarp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "passkey_auths", indexes = @Index(name = "IDX_UID", columnList = "userId"),
        // unique for pubKey:
        uniqueConstraints = @UniqueConstraint(name = "UNI_PUBKEY", columnNames = { "pubKey" }))
public class PasskeyAuth extends AbstractEntity {

    @Column(nullable = false, updatable = false)
    public long userId;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_NAME)
    public String credentialId;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_NAME)
    public String device;

    @Column(nullable = false, updatable = false)
    public int alg;

    @JsonIgnore
    @Column(nullable = false, updatable = false, length = VAR_CHAR_AUTH_TOKEN)
    public String pubKey;

    /**
     * comma-separated string: "internal,hybrid"
     */
    @Column(nullable = false, updatable = false, length = VAR_CHAR_TAGS)
    public String transports;

}
