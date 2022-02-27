package com.itranswarp.oauth.provider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.itranswarp.oauth.OAuthAuthentication;

@Component
public class FacebookOAuthProvider extends AbstractOAuthProvider {

    @Component
    @ConfigurationProperties("spring.signin.oauth.facebook")
    public static class OAuthConfiguration extends AbstractOAuthConfiguration {

    }

    @Autowired
    OAuthConfiguration configuration;

    @Override
    public AbstractOAuthConfiguration getOAuthConfiguration() {
        return this.configuration;
    }

    @Override
    public String getAuthenticateUrl(String redirectUrl) {
        return String.format("https://xxxxxxxxx/authorize?client_id=%s&response_type=%s&redirect_uri=%s", this.configuration.getClientId(), "code",
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
    }

    @Override
    public OAuthAuthentication getAuthentication(String code, String state, String redirectUrl) throws Exception {
        return null;
    }

}
