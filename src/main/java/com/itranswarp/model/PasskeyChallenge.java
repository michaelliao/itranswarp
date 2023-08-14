package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "passkey_challenges", uniqueConstraints = @UniqueConstraint(name = "UNI_CLG", columnNames = { "challenge" }))
public class PasskeyChallenge extends AbstractEntity {

    @Column(nullable = false)
    public long expiresAt;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_AUTH_TOKEN)
    public String challenge;
}
