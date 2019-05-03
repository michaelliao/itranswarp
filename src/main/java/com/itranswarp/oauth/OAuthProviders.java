package com.itranswarp.oauth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.oauth.provider.AbstractOAuthConfiguration;
import com.itranswarp.oauth.provider.AbstractOAuthProvider;

@Component
public class OAuthProviders {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	List<AbstractOAuthProvider> allOAuthProviders;

	List<AbstractOAuthProvider> enabledOAuthProviders;

	Map<String, AbstractOAuthProvider> enabledOAuthProviderMap;

	Map<String, AbstractOAuthConfiguration> enabledOAuthConfigurationMap;

	@PostConstruct
	public void init() {
		this.enabledOAuthProviders = this.allOAuthProviders.stream().filter(p -> p.isEnabled())
				.collect(Collectors.toList());
		this.enabledOAuthProviderMap = this.enabledOAuthProviders.stream().map(p -> {
			logger.info("Found OAuth provider: " + p.getProviderId());
			return p;
		}).collect(Collectors.toMap(AbstractOAuthProvider::getProviderId, p -> p));

		var map = new LinkedHashMap<String, AbstractOAuthConfiguration>();
		this.enabledOAuthProviders.stream()
				.map(provider -> new OAuthInfo(provider.getProviderId(), provider.getOAuthConfiguration())).sorted()
				.forEach(oi -> {
					map.put(oi.oauthProviderId, oi.oauthConfiguration);
				});
		this.enabledOAuthConfigurationMap = map;
	}

	public List<AbstractOAuthProvider> getOAuthProviders() {
		return this.enabledOAuthProviders;
	}

	public Map<String, AbstractOAuthConfiguration> getOAuthConfigurations() {
		return this.enabledOAuthConfigurationMap;
	}

	public AbstractOAuthProvider getOAuthProvider(String id) {
		AbstractOAuthProvider p = this.enabledOAuthProviderMap.get(id);
		if (p == null) {
			throw new ApiException(ApiError.PARAMETER_INVALID, "provider", "Invalid OAuth provider: " + id);
		}
		return p;
	}

	static class OAuthInfo implements Comparable<OAuthInfo> {
		final String oauthProviderId;
		final AbstractOAuthConfiguration oauthConfiguration;

		OAuthInfo(String oauthProviderId, AbstractOAuthConfiguration oauthConfiguration) {
			this.oauthProviderId = oauthProviderId;
			this.oauthConfiguration = oauthConfiguration;
		}

		@Override
		public int compareTo(OAuthInfo o) {
			return this.oauthConfiguration.getName().compareTo(o.oauthConfiguration.getName());
		}
	}
}
