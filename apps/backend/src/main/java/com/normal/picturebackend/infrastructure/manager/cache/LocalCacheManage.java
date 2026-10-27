package com.normal.picturebackend.infrastructure.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 本地缓存 caffeine 管理通过注入bean的方式进行统一的本地缓存
 */
@Component
public class LocalCacheManage {
    private static final String CACHE_KEY_PREFIX = "local_cache:";

    private Cache<String, String> CACHE;

    @PostConstruct
    public void init() {
        CACHE = Caffeine.newBuilder()
                .initialCapacity(10_000)
                // 超市时间 10分钟
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    public Cache<String, String> getCache() {
        return this.CACHE;
    }

    public void putCache(String key, String value) {
        CACHE.put(CACHE_KEY_PREFIX + key, value);
    }

    public String getCache(String key) {
        return CACHE.getIfPresent(CACHE_KEY_PREFIX + key);
    }

    // 清空缓存
    public void clearCache() {
        CACHE.invalidateAll();
    }


}
