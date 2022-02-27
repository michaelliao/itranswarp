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

@Component
public class WeiboOAuthProvider extends AbstractOAuthProvider {

    @Component
    @ConfigurationProperties("spring.signin.oauth.weibo")
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
        return String.format("https://api.weibo.com/oauth2/authorize?client_id=%s&response_type=%s&redirect_uri=%s", this.configuration.getClientId(), "code",
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
    }

    @Override
    public OAuthAuthentication getAuthentication(String code, String state, String redirectUrl) throws Exception {
        String[] queries = new String[] { // request body
                "client_id=" + this.configuration.getClientId(), // client id
                "client_secret=" + this.configuration.getClientSecret(), // client secret
                "grant_type=authorization_code", // grant type
                "code=" + code, // code
                "redirect_uri=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) };
        String postData = String.join("&", queries);
        HttpRequest requestAccessToken = HttpRequest.newBuilder().uri(new URI("https://api.weibo.com/oauth2/access_token")).timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(postData, StandardCharsets.UTF_8)).build();
        HttpResponse<String> responseAccessToken = this.httpClient.send(requestAccessToken, HttpResponse.BodyHandlers.ofString());
        if (responseAccessToken.statusCode() != 200) {
            throw new IOException("Bad response: " + responseAccessToken.statusCode());
        }
        WeiboAuth auth = JsonUtil.readJson(responseAccessToken.body(), WeiboAuth.class);
        HttpRequest requestUser = HttpRequest.newBuilder().uri(new URI("https://api.weibo.com/2/users/show.json?uid=" + auth.uid)).timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", "application/json").header("Authorization", "OAuth2 " + auth.access_token).GET().build();
        HttpResponse<String> responseUser = this.httpClient.send(requestUser, HttpResponse.BodyHandlers.ofString());
        if (responseUser.statusCode() != 200) {
            throw new IOException("Bad response: " + responseUser.statusCode());
        }
        WeiboUser user = JsonUtil.readJson(responseUser.body(), WeiboUser.class);
        return new OAuthAuthentication() {
            @Override
            public String getAuthenticationId() {
                return auth.uid;
            }

            @Override
            public String getAccessToken() {
                return auth.access_token;
            }

            @Override
            public Duration getExpires() {
                return Duration.ofSeconds(auth.expires_in);
            }

            @Override
            public String getName() {
                return user.screen_name;
            }

            @Override
            public String getProfileUrl() {
                return "https://weibo.com/" + (user.domain == null ? user.idstr : user.domain);
            }

            @Override
            public String getImageUrl() {
                if (user.profile_image_url != null && user.profile_image_url.startsWith("http://")) {
                    return "https://" + user.profile_image_url.substring(7);
                }
                return user.profile_image_url;
            }
        };
    }

    public static class WeiboAuth {

        public long expires_in;

        public String access_token;

        public String uid;

    }

    public static class WeiboUser {

        public String screen_name;

        public String domain;

        public String idstr;

        public String profile_image_url;
    }

}
