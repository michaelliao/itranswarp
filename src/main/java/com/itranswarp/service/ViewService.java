package com.itranswarp.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.redis.RedisService;

@Component
public class ViewService {

    static final String KEY_VIEWS = "__views__";

    @Autowired
    RedisService redisService;

    @Autowired
    ViewDbService viewDbService;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    public long increaseArticleViews(long id) {
        long value = this.redisService.hincrby(KEY_VIEWS, id);
        if (value % 1000 == 0) {
            executor.submit(() -> {
                this.viewDbService.updateArticleViews(id, value);
            });
        }
        return value;
    }

    public long increaseWikiViews(long id) {
        long value = this.redisService.hincrby(KEY_VIEWS, id);
        if (value % 1000 == 0) {
            executor.submit(() -> {
                this.viewDbService.updateWikiViews(id, value);
            });
        }
        return value;
    }

    public long increaseWikiPageViews(long id) {
        long value = this.redisService.hincrby(KEY_VIEWS, id);
        if (value % 1000 == 0) {
            executor.submit(() -> {
                this.viewDbService.updateWikiPageViews(id, value);
            });
        }
        return value;
    }

    public long[] getViews(Object... ids) {
        var kvs = this.redisService.hmget(KEY_VIEWS, ids);
        if (kvs == null) {
            return new long[ids.length];
        }
        return kvs.stream().mapToLong(kv -> {
            if (kv.hasValue()) {
                String v = kv.getValue();
                return Long.parseLong(v);
            }
            return 0;
        }).toArray();
    }

    public long getArticleViews(long id) {
        String value = this.redisService.hget(KEY_VIEWS, id);
        return value == null ? 0 : Long.parseLong(value);
    }

    public long getWikiViews(long id) {
        String value = this.redisService.hget(KEY_VIEWS, id);
        return value == null ? 0 : Long.parseLong(value);
    }

    public long getWikiPageViews(long id) {
        String value = this.redisService.hget(KEY_VIEWS, id);
        return value == null ? 0 : Long.parseLong(value);
    }

}
