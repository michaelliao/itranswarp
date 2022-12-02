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
public class BilibiliOAuthProvider extends AbstractOAuthProvider {

    @Component
    @ConfigurationProperties("spring.signin.oauth.bilibili")
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
        return String.format("https://passport.bilibili.com/register/pc_oauth2.html#/?client_id=%s&return_url=%s&response_type=%s&state=%s",
                this.configuration.getClientId(), URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8), "code", RandomUtil.createRandomString(12));
    }

    @Override
    public OAuthAuthentication getAuthentication(String code, String state, String redirectUrl) throws Exception {
        String[] queries = new String[] { // request body
                "client_id=" + this.configuration.getClientId(), // client id
                "client_secret=" + this.configuration.getClientSecret(), // client secret
                "grant_type=authorization_code", // grant type
                "code=" + code // code
        };
        String postData = String.join("&", queries);
        HttpRequest requestAccessToken = HttpRequest.newBuilder().uri(new URI("https://api.bilibili.com/x/account-oauth2/v1/token")).timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(postData, StandardCharsets.UTF_8)).build();
        HttpResponse<String> responseAccessToken = this.httpClient.send(requestAccessToken, HttpResponse.BodyHandlers.ofString());
        if (responseAccessToken.statusCode() != 200) {
            throw new IOException("Bad response: " + responseAccessToken.statusCode());
        }
        BilibiliAuth auth = JsonUtil.readJson(responseAccessToken.body(), BilibiliAuth.class);
        if (auth.code != 0 || auth.data == null || auth.data.access_token == null || auth.data.expires_in <= 0) {
            throw new IOException("Bad response: auth.code = " + auth.code);
        }
        String userInfoUrl = String.format("http://member.bilibili.com/arcopen/fn/user/account/info?client_id=%s&access_token=%s",
                this.configuration.getClientId(), auth.data.access_token);
        HttpRequest requestUser = HttpRequest.newBuilder().uri(new URI(userInfoUrl)).timeout(DEFAULT_TIMEOUT).GET().build();
        HttpResponse<String> responseUser = this.httpClient.send(requestUser, HttpResponse.BodyHandlers.ofString());
        if (responseUser.statusCode() != 200) {
            throw new IOException("Bad response: " + responseUser.statusCode());
        }
        BilibiliUser user = JsonUtil.readJson(responseUser.body(), BilibiliUser.class);
        if (user.code != 0 || user.data == null || user.data.openid == null) {
            throw new IOException("Bad response: user.code = " + user.code);
        }
        return new OAuthAuthentication() {
            @Override
            public String getAuthenticationId() {
                return user.data.openid;
            }

            @Override
            public String getAccessToken() {
                return auth.data.access_token;
            }

            @Override
            public Duration getExpires() {
                // fix Bilibili strange expires:
                long epoch = System.currentTimeMillis() / 1000;
                if (auth.data.expires_in > epoch) {
                    return Duration.ofSeconds(auth.data.expires_in - epoch);
                }
                return Duration.ofSeconds(auth.data.expires_in);
            }

            @Override
            public String getName() {
                return user.data.name;
            }

            @Override
            public String getProfileUrl() {
                return "https://www.bilibili.com";
            }

            @Override
            public String getImageUrl() {
                return user.data.face;
            }
        };
    }

    public static class BilibiliAuth {
        public long code;
        public BilibiliAuthData data;
    }

    public static class BilibiliAuthData {
        public long expires_in;
        public String access_token;
    }

    public static class BilibiliUser {
        public long code;
        public BilibiliUserData data;
    }

    public static class BilibiliUserData {
        public String name;
        public String face;
        public String openid;
    }
}
