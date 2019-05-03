package com.itranswarp.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public final class IdUtil {

	private static final Logger logger = LoggerFactory.getLogger(IdUtil.class);

	private static final Pattern PATTERN_LONG_ID = Pattern.compile("^([0-9]{15})([0-9a-f]{32})([0-9a-f]{3})$");

	private static final Pattern PATTERN_HOSTNAME = Pattern.compile("^.*\\D+([0-9]+)$");

	private static final long OFFSET = LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.of("Z")).toEpochSecond();

	private static final long MAX_NEXT = 0b11111_11111111_111L;

	private static final long SHARD_ID = getServerIdAsLong();

	private static long offset = 0;

	private static long lastEpoch = 0;

	public static long nextId() {
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
		return generateId(epochSecond, next, SHARD_ID);
	}

	private static void reset() {
		offset = 0;
	}

	private static long generateId(long epochSecond, long next, long shardId) {
		return ((epochSecond - OFFSET) << 21) | (next << 5) | shardId;
	}

	private static long getServerIdAsLong() {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			Matcher matcher = PATTERN_HOSTNAME.matcher(hostname);
			if (matcher.matches()) {
				long n = Long.parseLong(matcher.group(1));
				if (n >= 0 && n < 8) {
					logger.info("detect server id from host name {}: {}.", hostname, n);
					return n;
				}
			}
		} catch (UnknownHostException e) {
			logger.warn("unable to get host name. set server id = 0.");
		}
		return 0;
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
