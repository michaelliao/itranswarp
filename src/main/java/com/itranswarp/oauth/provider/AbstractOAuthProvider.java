package com.itranswarp.oauth.provider;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itranswarp.enums.AuthProviderType;
import com.itranswarp.oauth.OAuthAuthentication;

public abstract class AbstractOAuthProvider {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

	public abstract AuthProviderType getProvider();

	public abstract String getAuthenticateUrl(String redirectUrl);

	public abstract OAuthAuthentication getAuthentication(String code, String redirectUrl) throws Exception;

	protected final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(DEFAULT_TIMEOUT)
			.followRedirects(HttpClient.Redirect.NEVER).build();
}
