package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "single_pages")
public class SinglePage extends AbstractEntity {

	@Column(nullable = false, length = VAR_ID)
	public String textId;

	@Column(nullable = false, length = VAR_CHAR_TAGS)
	public String tags;

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false)
	public long publishAt;

}
