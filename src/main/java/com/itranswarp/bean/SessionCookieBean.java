package com.itranswarp.bean;

import com.itranswarp.enums.AuthProviderType;
import com.itranswarp.util.HashUtil;

public class SessionCookieBean {

	public final AuthProviderType provider;
	public final String authId;
	public final long expiresAt;
	public final String hash;

	public SessionCookieBean(AuthProviderType provider, String authId, long expiresAt, String hash) {
		this.provider = provider;
		this.authId = authId;
		this.expiresAt = expiresAt;
		this.hash = hash;
	}

	public boolean validate(String token, String hmacKey) {
		String payload = this.provider.name() + ":" + this.authId + ":" + this.expiresAt + ":" + token;
		String expectedHash = HashUtil.hmacSha256(payload, hmacKey);
		return expectedHash.equals(this.hash);
	}
}
