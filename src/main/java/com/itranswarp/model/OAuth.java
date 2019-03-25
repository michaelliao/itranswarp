package com.itranswarp.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "oauths", uniqueConstraints = @UniqueConstraint(name = "UNI_AUTH", columnNames = {
		"authProviderId,authId" }))
public class OAuth extends AbstractEntity {

	@Column(nullable = false, updatable = false)
	public long userId;

	@Column(nullable = false, updatable = false, length = VAR_ENUM)
	public String authProviderId;

	/**
	 * Authentication id from 3rd-party. NEVER changed.
	 */
	@Column(nullable = false, updatable = false, length = VAR_CHAR_AUTH_ID)
	public String authId;

	/**
	 * OAuth access token, updated by 3rd-party.
	 */
	@Column(nullable = false, length = VAR_CHAR_AUTH_TOKEN)
	public String authToken;

	/**
	 * OAuth access token expires time, updated by 3rd-party.
	 */
	@Column(nullable = false)
	public long expiresAt;

}
