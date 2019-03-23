package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.itranswarp.enums.RefType;

@Entity
@Table(name = "topics")
public class Topic extends AbstractEntity {

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String boardId;

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String userId;

	@Column(nullable = false, length = VAR_CHAR_NAME)
	public String userName;

	@Column(nullable = false, length = VAR_CHAR_URL)
	public String userImageUrl;

	@Column(nullable = false, updatable = false, length = VAR_ENUM)
	public RefType refType;

	@Column(nullable = false, updatable = false, length = VAR_ID)
	public String refId;

	@Column(nullable = false)
	public long replyNumber;

	@Column(nullable = false)
	public boolean locked;

	@Column(nullable = false, updatable = false, length = VAR_CHAR_NAME)
	public String name;

	@Column(nullable = false, updatable = false, columnDefinition = "TEXT")
	public String content;

}
