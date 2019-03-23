package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "replies")
public class Reply extends AbstractEntity {

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String topicId;

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String userId;

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String userName;

	@Column(nullable = false, length = VAR_CHAR_URL)
	public String userImageUrl;

	@Column(nullable = false, updatable = false, columnDefinition = "TEXT")
	public String content;

}
