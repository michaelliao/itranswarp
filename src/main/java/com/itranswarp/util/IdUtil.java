package com.itranswarp.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.itranswarp.redis.RedisService;

/**
 * 53 bits unique id:
 *
 * |--------|--------|--------|--------|--------|--------|--------|--------|
 * |00000000|00011111|11111111|11111111|11111111|11111111|11111111|11111111|
 * |--------|---xxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxx-----|--------|--------|
 * |--------|--------|--------|--------|--------|---xxxxx|xxxxxxxx|xxx-----|
 * |--------|--------|--------|--------|--------|--------|--------|---xxxxx|
 *
 * Maximum ID = 11111_11111111_11111111_11111111_11111111_11111111_11111111
 *
 * Maximum TS = 11111_11111111_11111111_11111111_111
 *
 * Maximum NT = ----- -------- -------- -------- ---11111_11111111_111 = 65535
 *
 * Maximum SH = ----- -------- -------- -------- -------- -------- ---11111 = 31
 *
 * It can generate 64k unique id per IP and up to 2106-02-07T06:28:15Z.
 */
@Component
public final class IdUtil {

    private static final Logger logger = LoggerFactory.getLogger(IdUtil.class);

    private static final Pattern PATTERN_LONG_ID = Pattern.compile("^([0-9]{15})([0-9a-f]{32})([0-9a-f]{3})$");

    private static final long OFFSET = LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.of("Z")).toEpochSecond();

    private static final long MAX_NEXT = 0b11111_11111111_111L;

    private static long shardingId = -1;

    private static long offset = 0;

    private static long lastEpoch = 0;

    @Autowired
    RedisService redisService;

    private String clientId;
    private String luaApplyHash;
    private String luaRenewHash;

    @PostConstruct
    public void init() {
        this.luaApplyHash = redisService.loadScriptFromClassPath("/redis/sharding-id-apply.lua");
        this.luaRenewHash = redisService.loadScriptFromClassPath("/redis/sharding-id-renew.lua");
        this.clientId = UUID.randomUUID().toString();
        int ret = redisService.executeScriptReturnInt(this.luaApplyHash, new String[] { "31", this.clientId, "600" }).intValue();
        if (ret < 0) {
            throw new IllegalStateException("Unable to apply sharding id from redis.");
        }
        shardingId = ret;
        logger.info("init sharding id = {}, client id = {}", shardingId, this.clientId);
    }

    @Scheduled(initialDelay = 360_000, fixedDelay = 360_000)
    public void scheduledRenew() {
        try {
            int ret = redisService.executeScriptReturnInt(this.luaRenewHash, new String[] { String.valueOf(shardingId), this.clientId, "600" }).intValue();
            if (ret < 0) {
                throw new IllegalStateException("Unable to renew sharding id from redis.");
            }
            logger.info("renewed sharding id: {}", shardingId);
        } catch (Exception e) {
            logger.error("unable renew sharding id for client " + clientId, e);
        }
    }

    public static long nextId() {
        if (shardingId < 0) {
            throw new IllegalStateException("Sharding id not applied.");
        }
        return nextId(System.currentTimeMillis() / 1000);
    }

    private static synchronized long nextId(long epochSecond) {
        if (epochSecond < lastEpoch) {
            // warning: clock is turn back:
            logger.warn("clock is back: " + epochSecond + " from previous:" + lastEpoch);
            epochSecond = lastEpoch;
        }
        if (lastEpoch != epochSecond) {
            lastEpoch = epochSecond;
            reset();
        }
        offset++;
        long next = offset & MAX_NEXT;
        if (next == 0) {
            logger.warn("maximum id reached in 1 second in epoch: " + epochSecond);
            return nextId(epochSecond + 1);
        }
        return generateId(epochSecond, next, shardingId);
    }

    private static void reset() {
        offset = 0;
    }

    private static long generateId(long epochSecond, long next, long shardId) {
        return ((epochSecond - OFFSET) << 21) | (next << 5) | shardId;
    }

    public static long stringIdToLongId(String stringId) {
        // a stringId id is composed as timestamp (15) + uuid (32) + serverId (000~fff).
        Matcher matcher = PATTERN_LONG_ID.matcher(stringId);
        if (matcher.matches()) {
            long epoch = Long.parseLong(matcher.group(1)) / 1000;
            String uuid = matcher.group(2);
            byte[] sha1 = HashUtil.sha1AsBytes(uuid);
            long next = ((sha1[0] << 24) | (sha1[1] << 16) | (sha1[2] << 8) | sha1[3]) & MAX_NEXT;
            long serverId = Long.parseLong(matcher.group(3), 16);
            return generateId(epoch, next, serverId);
        }
        throw new IllegalArgumentException("Invalid id: " + stringId);
    }
}
