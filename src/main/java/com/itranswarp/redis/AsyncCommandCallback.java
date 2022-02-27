package com.itranswarp.redis;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.api.async.RedisAsyncCommands;

@FunctionalInterface
public interface AsyncCommandCallback<T> {

    CompletableFuture<T> doInConnection(RedisAsyncCommands<String, String> commands);

}
