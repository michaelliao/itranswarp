package com.itranswarp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import com.itranswarp.bean.AbstractBean;
import com.itranswarp.util.IdUtil;

@MappedSuperclass
public abstract class AbstractEntity extends AbstractBean {

    public static final int VAR_ENUM = 32;

    public static final int VAR_CHAR_DATE = 10;

    public static final int VAR_CHAR_HASH = 64;

    public static final int VAR_CHAR_EMAIL = 100;

    public static final int VAR_CHAR_TAGS = 100;

    public static final int VAR_CHAR_NAME = 100;

    public static final int VAR_CHAR_MIME = 100;

    public static final int VAR_CHAR_AUTH_ID = 255;

    public static final int VAR_CHAR_AUTH_TOKEN = 255;

    public static final int VAR_CHAR_DESCRIPTION = 1000;

    public static final int VAR_CHAR_URL = 1000;

    public static final int TEXT = 65535; // 64K

    public static final int MEDIUM_TEXT = 524287; // 512K

    @Id
    @Column(nullable = false, updatable = false)
    public long id;

    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Column(nullable = false)
    public long updatedAt;

    @Column(nullable = false)
    public long version;

    // hook for pre-insert:
    @PrePersist
    void preInsert() {
        if (this.id == 0L) {
            this.id = IdUtil.nextId();
        }
        if (this.createdAt == 0L) {
            this.createdAt = this.updatedAt = System.currentTimeMillis();
        } else if (this.updatedAt == 0L) {
            this.updatedAt = System.currentTimeMillis();
        }
    }

    // hook for pre-update:
    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
        this.version++;
    }
}
