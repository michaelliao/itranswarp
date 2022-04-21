package com.itranswarp.redis;

import com.redis.lettucemod.api.sync.RedisModulesCommands;

@FunctionalInterface
public interface SyncCommandCallback<T> {

    T doInConnection(RedisModulesCommands<String, String> commands);
}
