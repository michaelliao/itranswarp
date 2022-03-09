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
public class QQOAuthProvider extends AbstractOAuthProvider {

    @Component
    @ConfigurationProperties("spring.signin.oauth.qq")
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
        return String.format("https://graph.qq.com/oauth2.0/authorize?client_id=%s&response_type=%s&redirect_uri=%s", this.configuration.getClientId(), "code",
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));
    }

    @Override
    public OAuthAuthentication getAuthentication(String code, String state, String redirectUrl) throws Exception {
        final String uriForAccessToken = new StringBuilder(128)
                // fixed url:
                .append("https://graph.qq.com/oauth2.0/token?fmt=json&grant_type=authorization_code&client_id=") //
                .append(this.configuration.getClientId()) // client_id
                .append("&client_secret=").append(this.configuration.getClientSecret()) // client_secret
                .append("&redirect_uri=").append(URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)) // redirect_uri
                .append("&code=").append(code) // code
                .toString();
        HttpRequest requestAccessToken = HttpRequest.newBuilder().uri(new URI(uriForAccessToken)).timeout(DEFAULT_TIMEOUT).GET().build();
        HttpResponse<String> responseAccessToken = this.httpClient.send(requestAccessToken, HttpResponse.BodyHandlers.ofString());
        if (responseAccessToken.statusCode() != 200) {
            throw new IOException("Bad response: " + responseAccessToken.statusCode());
        }
        final QQAuth auth = JsonUtil.readJson(responseAccessToken.body(), QQAuth.class);
        if (auth.error != 0) {
            throw new IOException("QQ Authenticate failed when get access token.");
        }

        String uriForGetOpenId = new StringBuilder(128).append("https://graph.qq.com/oauth2.0/me?fmt=json&access_token=").append(auth.access_token).toString();
        HttpRequest requestOpenId = HttpRequest.newBuilder().uri(new URI(uriForGetOpenId)).timeout(DEFAULT_TIMEOUT).GET().build();
        HttpResponse<String> responseOpenId = this.httpClient.send(requestOpenId, HttpResponse.BodyHandlers.ofString());
        if (responseOpenId.statusCode() != 200) {
            throw new IOException("Bad response: " + responseOpenId.statusCode());
        }
        final QQOpenId openId = JsonUtil.readJson(responseOpenId.body(), QQOpenId.class);
        if (openId.error != 0) {
            throw new IOException("QQ Authenticate failed when get open id.");
        }

        final String uriForGetUser = new StringBuilder(128) //
                .append("https://graph.qq.com/user/get_user_info?oauth_comsumer_key=") // prefix
                .append(this.getOAuthConfiguration().getClientId()) // client_id
                .append("&appid=").append(this.getOAuthConfiguration().getClientId()) // appid
                .append("&access_token=").append(auth.access_token) // access_token
                .append("&openid=").append(openId.openid) // openid
                .toString();
        HttpRequest requestUser = HttpRequest.newBuilder().uri(new URI(uriForGetUser)).timeout(DEFAULT_TIMEOUT).GET().build();
        HttpResponse<String> responseUser = this.httpClient.send(requestUser, HttpResponse.BodyHandlers.ofString());
        if (responseUser.statusCode() != 200) {
            throw new IOException("Bad response: " + responseUser.statusCode());
        }
        QQUser user = JsonUtil.readJson(responseUser.body(), QQUser.class);

        return new OAuthAuthentication() {
            @Override
            public String getAuthenticationId() {
                return openId.openid;
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
                return user.nickname;
            }

            @Override
            public String getProfileUrl() {
                return "https://qzone.qq.com/";
            }

            @Override
            public String getImageUrl() {
                if (user.figureurl_qq_2 != null && !user.figureurl_qq_2.isEmpty()) {
                    return user.figureurl_qq_2;
                }
                return user.figureurl_qq_1;
            }
        };
    }

    public static class QQAuth {

        public int error;

        public long expires_in;

        public String access_token;

    }

    public static class QQOpenId {

        public int error;

        public String openid;

    }

    public static class QQUser {

        public int ret;
        public String msg;

        public String nickname;

        public String figureurl; // 30x30 qzone
        public String figureurl_1; // 50x50 qzone
        public String figureurl_qq_1; // 40x40 qq
        public String figureurl_qq_2; // 100x100 qq

    }
}
