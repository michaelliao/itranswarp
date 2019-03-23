package com.itranswarp.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate unique id as string.
 * 
 * @author liaoxuefeng
 */
public class IdUtil {

	private static final Logger logger = LoggerFactory.getLogger(IdUtil.class);

	private static final long MAX_SEQUENCE = 1679615L;

	private static final AtomicLong sequencer = new AtomicLong(0);

	private static final Pattern PATTERN_LONG_ID = Pattern.compile("^([0-9]{15})([0-9a-f]{32})([0-9a-f]{3})$");

	private static final Pattern PATTERN_HOSTNAME = Pattern.compile("^.*\\D+([0-9]+)$");

	static final String serverId = getServerId();

	static String getServerId() {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			Matcher matcher = PATTERN_HOSTNAME.matcher(hostname);
			if (matcher.matches()) {
				long n = Long.parseLong(matcher.group(1));
				if (n >= 0 && n < 36) {
					logger.info("detect server id from host name {}: {}.", hostname, n);
					return Long.toString(n, 36);
				}
			}
		} catch (UnknownHostException e) {
			logger.warn("unable to get host name.");
		}
		return "0";
	}

	/**
	 * Generate next id as string. composed by:
	 * 
	 * 7 chars timestamp as seconds: 0000000 ~ zzzzzzz, up to 4453-04-05T15:21:35Z.
	 * 
	 * 4 chars sequence: 0000 ~ zzzz (0 ~ 1679615).
	 * 
	 * 1 char serverId: 0 ~ z, (0 ~ 35).
	 * 
	 * @return
	 */
	public static String nextId() {
		long epoch = System.currentTimeMillis() / 1000L;
		long seq = sequencer.incrementAndGet() % MAX_SEQUENCE;
		return nextId(Long.toString(epoch, 36), Long.toString(seq, 36), serverId);
	}

	static String nextId(String ts, String seq, String serverId) {
		StringBuilder sb = new StringBuilder(12);
		for (int i = 0; i < 7 - ts.length(); i++) {
			sb.append('0');
		}
		sb.append(ts);
		for (int i = 0; i < 4 - seq.length(); i++) {
			sb.append('0');
		}
		sb.append(seq);
		sb.append(serverId);
		return sb.toString();
	}

	public static String longIdToShortId(String longId) {
		// a long id is composed as timestamp (15) + uuid (32) + serverId (000~fff).
		Matcher matcher = PATTERN_LONG_ID.matcher(longId);
		if (matcher.matches()) {
			long timestamp = Long.parseLong(matcher.group(1));
			String uuid = matcher.group(2);
			long serverId = Long.parseLong(matcher.group(3), 16);
			return nextId(Long.toString(timestamp / 1000L, 36), HashUtil.sha256(uuid).substring(0, 4),
					Long.toString(serverId, 36));
		}
		throw new IllegalArgumentException("Invalid id.");
	}

}
