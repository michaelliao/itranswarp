package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends AbstractSortableEntity {

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false, length = VAR_ENUM)
	public String tag;

	@Column(nullable = false, length = VAR_CHAR_DESCRIPTION)
	public String description;

}
