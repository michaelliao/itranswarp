package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.itranswarp.util.IdUtil;

@MappedSuperclass
public abstract class AbstractEntity {

	public static final int VAR_ID = 12;

	public static final int VAR_ENUM = 32;

	public static final int VAR_CHAR_HASH = 64;

	public static final int VAR_CHAR_EMAIL = 100;

	public static final int VAR_CHAR_TAGS = 100;

	public static final int VAR_CHAR_NAME = 100;

	public static final int VAR_CHAR_MIME = 100;

	public static final int VAR_CHAR_AUTH_ID = 255;

	public static final int VAR_CHAR_AUTH_TOKEN = 255;

	public static final int VAR_CHAR_DESCRIPTION = 1000;

	public static final int VAR_CHAR_URL = 1000;

	@Id
	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String id;

	@Column(nullable = false, updatable = false)
	public long createdAt;

	@Column(nullable = false)
	public long updatedAt;

	@Column(nullable = false)
	public long version;

	// hook for pre-insert:
	@PrePersist
	void preInsert() {
		if (this.id == null) {
			this.id = IdUtil.nextId();
		}
		if (this.createdAt == 0) {
			this.createdAt = this.updatedAt = System.currentTimeMillis();
		} else if (this.updatedAt == 0) {
			this.updatedAt = System.currentTimeMillis();
		}
	}

	// hook for pre-update:
	@PreUpdate
	void preUpdate() {
		if (this.updatedAt == 0) {
			this.updatedAt = System.currentTimeMillis();
		}
		this.version++;
	}

}
