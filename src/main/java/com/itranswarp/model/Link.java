package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "links")
public class Link extends AbstractEntity {

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false, length = VAR_CHAR_URL)
	public String url;

	@Transient
	public String getShortenUrl() {
		return "/link/" + this.id;
	}
}
