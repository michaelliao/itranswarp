package com.itranswarp.oauth.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.oauth.OAuthAuthentication;
import com.itranswarp.oauth.provider.WeiboOAuthProvider;
import com.itranswarp.oauth.provider.WeiboOAuthProvider.OAuthConfiguration;
import com.itranswarp.util.JsonUtil;

public class WeiboOAuthProviderTest {

	WeiboOAuthProvider provider;

	@BeforeEach
	void setUp() throws Exception {
		provider = new WeiboOAuthProvider();
		provider.configuration = new OAuthConfiguration();
		provider.configuration.setClientId("1391944217");
		provider.configuration.setClientSecret(System.getenv("TEST_WEIBO_CLIENT_SECRET"));
	}

	@Test
	void testGetAuthenticateUrl() {
		String url = "https://api.weibo.com/oauth2/authorize?client_id=1391944217&response_type=code&redirect_uri=http%3A%2F%2Fredirect.com%2Fpath%2Fto%3Fcallback%3Dok%26t%3D12345";
		assertEquals(url, provider.getAuthenticateUrl("http://redirect.com/path/to?callback=ok&t=12345"));
	}

	@Test
	void testGetAuthentication() throws Exception {
		String redirectUrl = provider.getAuthenticateUrl("https://www.liaoxuefeng.com/callback");
		System.out.println("copy url to get code:\n" + redirectUrl);
		String code = "xxxxx";
		OAuthAuthentication auth = provider.getAuthentication(code, "", "https://www.liaoxuefeng.com/callback");
		System.out.println(JsonUtil.writeJson(auth));
	}

}
