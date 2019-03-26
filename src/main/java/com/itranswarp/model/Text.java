package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "texts", uniqueConstraints = @UniqueConstraint(name = "UNI_HASH", columnNames = { "hash" }))
public class Text extends AbstractEntity {

	@Column(nullable = false, updatable = false, length = VAR_CHAR_HASH)
	public String hash;

	@Column(nullable = false, updatable = false, columnDefinition = "TEXT")
	public String content;

}
