package com.itranswarp.redis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.itranswarp.util.ClassPathUtil;
import com.itranswarp.util.JsonUtil;

import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

@Component
public class RedisService {

	Logger logger = LoggerFactory.getLogger(getClass());

	final RedisClient redisClient;

	public RedisService(@Autowired RedisClient redisClient) {
		this.redisClient = redisClient;
	}

	@PreDestroy
	public void shutdown() {
		this.redisClient.shutdown();
	}

	public CompletableFuture<Long> publishAsync(String channel, String message) {
		return executeAsync(commands -> {
			return commands.publish(channel, message).toCompletableFuture();
		});
	}

	public void subscribe(String channel, Consumer<String> listener) {
		StatefulRedisPubSubConnection<String, String> conn = this.redisClient.connectPubSub();
		conn.addListener(new RedisPubSubAdapter<String, String>() {
			@Override
			public void message(String channel, String message) {
				listener.accept(message);
			}
		});
		conn.sync().subscribe(channel);
	}

	public String get(String key) {
		return executeSync(commands -> {
			String str = commands.get(key);
			if (str == null) {
				return null;
			}
			return str;
		});
	}

	public <T> T get(String key, Class<T> clazz) {
		return executeSync(commands -> {
			String str = commands.get(key);
			if (str == null) {
				return null;
			}
			return JsonUtil.readJson(str, clazz);
		});
	}

	public <T> T get(String key, TypeReference<T> type) {
		return executeSync(commands -> {
			String str = commands.get(key);
			if (str == null) {
				return null;
			}
			return JsonUtil.readJson(str, type);
		});
	}

	public void del(String key) {
		executeSync(commands -> {
			commands.del(key);
			return null;
		});
	}

	public void set(String key, Object obj) {
		executeSync(commands -> {
			commands.set(key, obj instanceof String ? (String) obj : JsonUtil.writeJson(obj));
			return null;
		});
	}

	public void set(String key, Object obj, long seconds) {
		executeSync(commands -> {
			commands.set(key, obj instanceof String ? (String) obj : JsonUtil.writeJson(obj));
			commands.expire(key, seconds);
			return null;
		});
	}

	public String hget(String key, Object field) {
		return executeSync(commands -> {
			String str = commands.hget(key, field.toString());
			if (str == null) {
				return null;
			}
			return str;
		});
	}

	public List<KeyValue<String, String>> hmget(String key, Object... fields) {
		return executeSync(commands -> {
			String[] strs = Arrays.stream(fields).map(f -> f.toString()).toArray(String[]::new);
			return commands.hmget(key, strs);
		});
	}

	public <T> T hget(String key, Object field, Class<T> clazz) {
		return executeSync(commands -> {
			String str = commands.hget(key, field.toString());
			if (str == null) {
				return null;
			}
			return JsonUtil.readJson(str, clazz);
		});
	}

	public <T> T hget(String key, Object field, TypeReference<T> type) {
		return executeSync(commands -> {
			String str = commands.hget(key, field.toString());
			if (str == null) {
				return null;
			}
			return JsonUtil.readJson(str, type);
		});
	}

	public void hdel(String key, Object field) {
		executeSync(commands -> {
			commands.hdel(key, field.toString());
			return null;
		});
	}

	public void hset(String key, Object field, Object obj) {
		executeSync(commands -> {
			commands.hset(key, field.toString(), obj instanceof String ? (String) obj : JsonUtil.writeJson(obj));
			return null;
		});
	}

	public void hsetAll(String key, Map<String, String> kv) {
		executeSync(commands -> {
			commands.del(key);
			kv.forEach((k, v) -> {
				commands.hset(key, k, v);
			});
			return null;
		});
	}

	public Map<String, String> hgetAll(String key) {
		return executeSync(commands -> {
			return commands.hgetall(key);
		});
	}

	public <T> Map<String, T> hgetAll(String key, Class<T> clazz) {
		return executeSync(commands -> {
			Map<String, String> map = commands.hgetall(key);
			if (map.isEmpty()) {
				return Map.of();
			}
			Map<String, T> result = new HashMap<>();
			map.forEach((field, str) -> {
				result.put(field, JsonUtil.readJson(str, clazz));
			});
			return result;
		});
	}

	public long hincrby(String key, Object field) {
		return executeSync(commands -> {
			return commands.hincrby(key, field.toString(), 1);
		});
	}

	/**
	 * Load Lua script from classpath file and return SHA as string.
	 *
	 * @param classpathFile Script path.
	 * @return SHA as string.
	 */
	public String loadScriptFromClassPath(String classpathFile) {
		String sha = executeSync(commands -> {
			try {
				return commands.scriptLoad(ClassPathUtil.readFile(classpathFile));
			} catch (IOException e) {
				throw new UncheckedIOException("load file from classpath failed: " + classpathFile, e);
			}
		});
		if (logger.isInfoEnabled()) {
			logger.info("loaded script {} from {}.", sha, classpathFile);
		}
		return sha;
	}

	public CompletableFuture<Long> hincrbyAsync(String key, Object field) {
		return executeAsync(commands -> {
			return commands.hincrby(key, field.toString(), 1).toCompletableFuture();
		});
	}

	public Number executeScriptReturnInt(String sha, String[] keys) {
		return executeSync(commands -> {
			return commands.evalsha(sha, ScriptOutputType.INTEGER, keys);
		});
	}

	public Number executeScriptReturnInt(String sha, String[] keys, String[] values) {
		return executeSync(commands -> {
			return commands.evalsha(sha, ScriptOutputType.INTEGER, keys, values);
		});
	}

	public <T> T executeSync(SyncCommandCallback<T> callback) {
		try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
			connection.setAutoFlushCommands(true);
			RedisCommands<String, String> commands = connection.sync();
			return callback.doInConnection(commands);
		}
	}

	public <T> CompletableFuture<T> executeBatchAsync(BatchAsyncCommandCallback<T> callback) {
		try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
			connection.setAutoFlushCommands(false);
			RedisAsyncCommands<String, String> commands = connection.async();
			CompletableFuture<T> future = callback.doInConnection(commands);
			commands.flushCommands();
			return future;
		}
	}

	public <T> CompletableFuture<T> executeAsync(AsyncCommandCallback<T> callback) {
		try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
			connection.setAutoFlushCommands(true);
			RedisAsyncCommands<String, String> commands = connection.async();
			return callback.doInConnection(commands);
		}
	}

}
