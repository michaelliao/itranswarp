package com.itranswarp.oauth;

import java.time.Duration;

public interface OAuthAuthentication {

    /**
     * Required: authentication id as identity for OAuth user.
     */
    String getAuthenticationId();

    /**
     * Required: OAuth access token.
     */
    String getAccessToken();

    /**
     * Optional: OAuth refresh token.
     */
    default String getRefreshToken() {
        return "";
    }

    /**
     * Required: expires time in seconds.
     */
    Duration getExpires();

    /**
     * Required: user display name.
     */
    String getName();

    /**
     * Optional: user profile url.
     */
    String getProfileUrl();

    /**
     * Optional: user image url.
     */
    String getImageUrl();

}
