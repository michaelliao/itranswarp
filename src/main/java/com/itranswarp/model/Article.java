package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "articles", indexes = @Index(name = "IDX_CAT_PUB", columnList = "categoryId,publishAt"))
public class Article extends AbstractEntity {

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String userId;

	@Column(nullable = false, length = VAR_ID)
	public String categoryId;

	/**
	 * Reference to attachment id.
	 */
	@Column(nullable = false, length = VAR_ID)
	public String imageId;

	@Column(nullable = false, length = VAR_ID)
	public String textId;

	@Column(nullable = false)
	public long views;

	@Column(nullable = false, length = VAR_CHAR_TAGS)
	public String tags;

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false, length = VAR_CHAR_DESCRIPTION)
	public String description;

	@Column(nullable = false)
	public long publishAt;

}
