package com.itranswarp.bean;

import com.itranswarp.util.HashUtil;

public class SessionCookieBean {

    public final String authProvider;
    public final long id;
    public final long expiresAt;
    public final String hash;

    public SessionCookieBean(String authProvider, long id, long expiresAt, String hash) {
        this.authProvider = authProvider;
        this.id = id;
        this.expiresAt = expiresAt;
        this.hash = hash;
    }

    public boolean validate(String token, String hmacKey) {
        String payload = this.authProvider + ":" + this.id + ":" + this.expiresAt + ":" + token;
        String expectedHash = HashUtil.hmacSha256(payload, hmacKey);
        return expectedHash.equals(this.hash);
    }
}
