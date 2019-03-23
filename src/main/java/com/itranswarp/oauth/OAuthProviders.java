package com.itranswarp.oauth;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.AuthProviderType;
import com.itranswarp.oauth.provider.AbstractOAuthProvider;

@Component
public class OAuthProviders {

	@Autowired(required = false)
	List<AbstractOAuthProvider> enabledOAuthProviders = List.of();

	Map<AuthProviderType, AbstractOAuthProvider> enabledOAuthProviderMap;

	@PostConstruct
	public void init() {
		this.enabledOAuthProviderMap = this.enabledOAuthProviders.stream()
				.collect(Collectors.toMap(AbstractOAuthProvider::getProvider, p -> p));
	}

	public AbstractOAuthProvider getOAuthProvider(AuthProviderType type) {
		AbstractOAuthProvider p = this.enabledOAuthProviderMap.get(type);
		if (p == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "provider", "Invalid OAuth provider: " + type);
		}
		return p;
	}

}
