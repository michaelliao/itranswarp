package com.itranswarp.service;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itranswarp.util.HashUtil;

@Component
public class EncryptService {

    @Value("${spring.security.encrypt.key:ChangeTheKeyWhenDeployProduction}")
    String encryptKey = "ChangeTheKeyWhenDeployProduction";

    String sessionHmacKey;

    @PostConstruct
    public void init() {
        this.sessionHmacKey = HashUtil.hmacSha256(this.encryptKey, "SessionHmacKey");
    }

    public String getSessionHmacKey() {
        return this.sessionHmacKey;
    }
}
