package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.itranswarp.enums.Role;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "UNI_EMAIL", columnNames = { "email" }))
public class User extends AbstractEntity {

    @Column(nullable = false, length = VAR_ENUM)
    public Role role;

    @Column(nullable = false, updatable = false, length = VAR_CHAR_EMAIL)
    public String email;

    @Column(nullable = false, length = VAR_CHAR_NAME)
    public String name;

    @Column(nullable = false, length = VAR_CHAR_URL)
    public String imageUrl;

    @Column(nullable = false)
    public long lockedUntil;

    @Override
    public String toString() {
        return String.format("{User: id=%s, role=%s, email=%s, name=%s, locakedUntil=%s}", this.id, this.role, this.email, this.name, this.lockedUntil);
    }
}
