package com.itranswarp.service;

import java.time.Instant;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.redis.RedisService;

@Component
public class RedisRateLimiter {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String shaScript;

	@Autowired
	RedisService redisService;

	@PostConstruct
	public void init() {
		this.shaScript = this.redisService.loadScriptFromClassPath("/redis/request_rate_limiter.lua");
		logger.info("load rate-limit-script: {}", this.shaScript);
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
		String[] values = new String[] { String.valueOf(limit), String.valueOf(burst),
				String.valueOf(Instant.now().getEpochSecond()), "1" };
		Number remaining = this.redisService.executeScriptReturnInt(this.shaScript, keys, values);
		return remaining.intValue();
	}
}
