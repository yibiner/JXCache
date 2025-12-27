package dev.yibin.jxcache.observer.exception;

/**
 * 表示指定 area/cacheName 在当前节点的 CacheManager 中不存在。
 *
 * <p>用于区分：缓存实例不存在 vs 缓存键不存在。</p>
 */
public class CacheNotFoundException extends RuntimeException {

    public CacheNotFoundException(String area, String cacheName) {
        super(String.format("Cache not found: area=%s, name=%s", area, cacheName));
    }
}

