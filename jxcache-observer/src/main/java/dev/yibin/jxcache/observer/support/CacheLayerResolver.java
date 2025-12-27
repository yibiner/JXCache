package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.Cache;

/**
 * 缓存层级解析策略。
 */
public interface CacheLayerResolver {

    boolean supports(Cache<Object, Object> raw);

    CacheLayers resolve(Cache<Object, Object> raw);
}

