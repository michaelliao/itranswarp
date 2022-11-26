package com.itranswarp.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itranswarp.bean.SessionCookieBean;
import com.itranswarp.model.EthAuth;
import com.itranswarp.model.LocalAuth;
import com.itranswarp.model.OAuth;

public class CookieUtil {

    static final Logger logger = LoggerFactory.getLogger(CookieUtil.class);

    static final String SESSION_COOKIE = "_session_";

    public static String encodeSessionCookie(OAuth auth, String hmacKey) {
        return encodeSessionCookie(auth.authProviderId, auth.id, auth.expiresAt, auth.authToken, hmacKey);
    }

    public static String encodeSessionCookie(LocalAuth auth, long expiresAt, String hmacKey) {
        return encodeSessionCookie("local", auth.id, expiresAt, auth.passwd, hmacKey);
    }

    public static String encodeSessionCookie(EthAuth auth, long expiresAt, String hmacKey) {
        return encodeSessionCookie("eth", auth.id, expiresAt, auth.address, hmacKey);
    }

    public static SessionCookieBean decodeSessionCookie(String str) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(str), StandardCharsets.UTF_8);
            String[] ss = raw.split("\\:");
            if (ss.length != 4) {
                return null;
            }
            long expires = Long.parseLong(ss[2]);
            if (expires <= System.currentTimeMillis()) {
                return null;
            }
            return new SessionCookieBean(ss[0], Long.parseLong(ss[1]), expires, ss[3]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cookie composed by:
     * 
     * payload = provider : authId : expiresAt : token
     * 
     * hash = sha256(payload, hmacKey)
     * 
     * cookie = base64(provider : authId : expiresAt : hash)
     */
    static String encodeSessionCookie(String authProviderId, long authId, long expiresAt, String token, String hmacKey) {
        String prefix = new StringBuilder(128).append(authProviderId).append(':').append(authId).append(':').append(expiresAt).toString();
        String payloadToHash = prefix + ":" + token;
        String hash = HashUtil.hmacSha256(payloadToHash, hmacKey);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((prefix + ":" + hash).getBytes(StandardCharsets.UTF_8));
    }

    public static String findSessionCookie(HttpServletRequest request) {
        Cookie[] cs = request.getCookies();
        if (cs == null) {
            return null;
        }
        for (Cookie c : cs) {
            if (SESSION_COOKIE.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    public static void deleteSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        logger.info("delete session cookie...");
        Cookie cookie = new Cookie(SESSION_COOKIE, "-deleted-");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        response.addCookie(cookie);
    }

    public static void setSessionCookie(HttpServletRequest request, HttpServletResponse response, String cookieStr, int maxAgeInSeconds) {
        logger.info("set session cookie: " + cookieStr);
        Cookie cookie = new Cookie(SESSION_COOKIE, cookieStr);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeInSeconds);
        cookie.setHttpOnly(true);
        cookie.setSecure(HttpUtil.isSecure(request));
        response.addCookie(cookie);
    }

}
