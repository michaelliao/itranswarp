package com.itranswarp.oauth.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.itranswarp.oauth.OAuthAuthentication;
import com.itranswarp.util.JsonUtil;
import com.itranswarp.util.RandomUtil;

@Component
public class GithubOAuthProvider extends AbstractOAuthProvider {

    @Component
    @ConfigurationProperties("spring.signin.oauth.github")
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
        String state = RandomUtil.createRandomString(20);
        return String.format("https://github.com/login/oauth/authorize?client_id=%s&response_type=%s&state=%s&redirect_uri=%s",
                this.configuration.getClientId(), "code", state, URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
    }

    @Override
    public OAuthAuthentication getAuthentication(String code, String state, String redirectUrl) throws Exception {
        String[] queries = new String[] { // request body
                "client_id=" + this.configuration.getClientId(), // client id
                "client_secret=" + this.configuration.getClientSecret(), // client secret
                "grant_type=authorization_code", // grant type
                "code=" + code, // code
                "state=" + state, // state
                "redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) };
        String postData = String.join("&", queries);
        HttpRequest requestAccessToken = HttpRequest.newBuilder().uri(new URI("https://github.com/login/oauth/access_token")).timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postData, StandardCharsets.UTF_8)).build();
        HttpResponse<String> responseAccessToken = this.httpClient.send(requestAccessToken, HttpResponse.BodyHandlers.ofString());
        if (responseAccessToken.statusCode() != 200) {
            throw new IOException("Bad response: " + responseAccessToken.statusCode());
        }
        GitHubAccessTokenResponse resp = JsonUtil.readJson(responseAccessToken.body(), GitHubAccessTokenResponse.class);
        final String accessToken = resp.access_token;
        // get user info:
        HttpRequest requestUser = HttpRequest.newBuilder().uri(new URI("https://api.github.com/user")).timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", "application/json").header("Authorization", "token " + accessToken).GET().build();
        HttpResponse<String> responseUser = this.httpClient.send(requestUser, HttpResponse.BodyHandlers.ofString());
        if (responseUser.statusCode() != 200) {
            throw new IOException("Bad response: " + responseUser.statusCode());
        }
        GitHubUser user = JsonUtil.readJson(responseUser.body(), GitHubUser.class);
        if (!"User".equals(user.type)) {
            throw new RuntimeException("Invalid user response type: " + user.type);
        }
        return new OAuthAuthentication() {
            @Override
            public String getAuthenticationId() {
                return user.node_id;
            }

            @Override
            public String getAccessToken() {
                return accessToken;
            }

            @Override
            public Duration getExpires() {
                // since github token never expires, set 7 days:
                return Duration.ofDays(7);
            }

            @Override
            public String getName() {
                return user.name;
            }

            @Override
            public String getProfileUrl() {
                return user.html_url;
            }

            @Override
            public String getImageUrl() {
                return user.avatar_url;
            }
        };
    }

    public static class GitHubAccessTokenResponse {
        public String access_token;
    }

    public static class GitHubUser {
        public String node_id; // global user id
        public String type; // "User"
        public String name;
        public String html_url;
        public String avatar_url;
    }
}
