package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "boards")
public class Board extends AbstractSortableEntity {

	@Column(nullable = false)
	public long topicNumber;

	@Column(nullable = false)
	public boolean locked;

	@Column(nullable = false, length = VAR_ENUM)
	public String tag;

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false, length = VAR_CHAR_DESCRIPTION)
	public String description;

}
