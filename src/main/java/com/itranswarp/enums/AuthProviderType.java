package com.itranswarp.enums;

public enum AuthProviderType {

	/**
	 * Authenticate by Sina Weibo.
	 */
	WEIBO(true),

	/**
	 * Authenticate by Tencent QQ.
	 */
	QQ(true),

	/**
	 * Authenticate by Facebook.
	 */
	FACEBOOK(true),

	/**
	 * Authenticate by Google.
	 */
	GOOGLE(true),

	/**
	 * Authenticate by Twitter.
	 */
	TWITTER(true),

	/**
	 * Local password authentication.
	 */
	LOCAL(false);

	public final boolean supportOAuth;

	AuthProviderType(boolean supportOAuth) {
		this.supportOAuth = supportOAuth;
	}

	@Override
	public String toString() {
		return String.format("{%s: supportOAuth=%s}", this.name(), this.supportOAuth);
	}
}
