package com.itranswarp.service;

import java.time.Instant;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.common.AbstractService;
import com.itranswarp.redis.RedisService;

@Component
public class RedisRateLimiter extends AbstractService {

    private String shaScript;

    @Autowired
    RedisService redisService;

    @PostConstruct
    public void init() {
        this.shaScript = this.redisService.loadScriptFromClassPath("/redis/request_rate_limiter.lua");
    }

    /**
     * Get rate limit by type, key, limit and burst. Return how many remaining left
     * including this response.
     * 
     * if >0: pass, 0 = denied.
     * 
     * @param type  Rate limit type, e.g. "signinApi"
     * @param key   Rate limit key, e.g. "user-123"
     * @param limit Rate limit per second.
     * @param burst Rate limit burst.
     * @return how many remaining left including this response.
     */
    public int getRateLimit(String type, String key, int limit, int burst) {
        String[] keys = new String[] { type, key };
        String[] values = new String[] { String.valueOf(limit), String.valueOf(burst), String.valueOf(Instant.now().getEpochSecond()), "1" };
        Number remaining = this.redisService.executeScriptReturnInt(this.shaScript, keys, values);
        return remaining.intValue();
    }
}
