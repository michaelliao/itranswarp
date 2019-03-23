package com.itranswarp.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itranswarp.common.ApiException;
import com.itranswarp.enums.ApiError;
import com.itranswarp.enums.AuthProviderType;
import com.itranswarp.model.LocalAuth;
import com.itranswarp.model.OAuth;
import com.itranswarp.model.User;
import com.itranswarp.oauth.OAuthAuthentication;
import com.itranswarp.oauth.OAuthProviders;
import com.itranswarp.oauth.provider.AbstractOAuthProvider;
import com.itranswarp.util.CookieUtil;
import com.itranswarp.util.HashUtil;
import com.itranswarp.util.HttpUtil;

@Controller
public class SignInController extends AbstractController {

	static final int LOCAL_EXPIRES_IN_SECONDS = 3600 * 24 * 7;
	static final long LOCAL_EXPIRES_IN_MILLIS = LOCAL_EXPIRES_IN_SECONDS * 1000L;

	@Autowired
	OAuthProviders oauthProviders;

	@PostMapping("/auth/signin")
	public String localSignIn(@RequestParam("email") String email, @RequestParam("password") String password,
			HttpServletRequest request, HttpServletResponse response) {
		// try find user by email:
		User user = userService.fetchUserByEmail(email.strip().toLowerCase());
		if (user == null) {
			throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Invalid email or password.");
		}
		// try find local auth by userId:
		LocalAuth auth = userService.fetchLocalAuthByUserId(user.id);
		if (auth == null) {
			throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Invalid email or password.");
		}
		// validate password:
		String expectedPassword = HashUtil.hmacSha256(password, auth.salt);
		if (!expectedPassword.equals(auth.passwd)) {
			throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Invalid email or password.");
		}
		// set cookie:
		String cookieStr = CookieUtil.encodeSessionCookie(auth, System.currentTimeMillis() + LOCAL_EXPIRES_IN_MILLIS,
				encryptService.getSessionHmacKey());
		CookieUtil.setSessionCookie(request, response, cookieStr, LOCAL_EXPIRES_IN_SECONDS);
		return "redirect:" + HttpUtil.getReferer(request);
	}

	@GetMapping("/auth/from/{provider}")
	public String oauthFrom(@PathVariable("provider") String providerName, HttpServletRequest request) {
		AuthProviderType providerType = AuthProviderType.valueOf(providerName);
		AbstractOAuthProvider provider = this.oauthProviders.getOAuthProvider(providerType);
		String url = HttpUtil.getScheme(request) + "://" + request.getServerName() + "/auth/callback/" + providerName;
		return provider.getAuthenticateUrl(url);
	}

	@GetMapping("/auth/callback/{provider}")
	public String oauthCallback(@PathVariable("provider") String providerName, @RequestParam("code") String code,
			HttpServletRequest request, HttpServletResponse response) {
		AuthProviderType providerType = AuthProviderType.valueOf(providerName);
		AbstractOAuthProvider provider = this.oauthProviders.getOAuthProvider(providerType);
		String url = HttpUtil.getScheme(request) + "://" + request.getServerName() + "/auth/callback/" + providerName;
		OAuthAuthentication authentication = null;
		try {
			authentication = provider.getAuthentication(code, url);
		} catch (Exception e) {
			throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, null, "Signin from OAuth failed.");
		}
		OAuth auth = this.userService.getOAuth(providerType, authentication);
		String cookieStr = CookieUtil.encodeSessionCookie(auth, encryptService.getSessionHmacKey());
		CookieUtil.setSessionCookie(request, response, cookieStr, (int) authentication.getExpires().toSeconds());
		return "redirect:" + HttpUtil.getReferer(request);
	}

	public String signOut(HttpServletRequest request, HttpServletResponse response) {
		CookieUtil.deleteSessionCookie(request, response);
		return "redirect:" + HttpUtil.getReferer(request);
	}
}
