package dev.yibin.jxcache.observer.introspector;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.embedded.CaffeineCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

/**
 * Caffeine 本地缓存内省器
 * 支持 CaffeineCache 类型的本地缓存扫描和查询
 */
@Component
@ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
public class CaffeineLocalCacheIntrospector extends AbstractLocalCacheIntrospector {

    @Override
    public boolean supports(Cache<?, ?> cache) {
        return cache instanceof CaffeineCache;
    }
    
    @Override
    protected Iterator<Map.Entry<Object, Object>> getEntryIterator(Cache<Object, Object> cache) {
        if (!(cache instanceof CaffeineCache)) {
            return null;
        }
        
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                extractNativeCaffeineCache((CaffeineCache<Object, Object>) cache);
        if (nativeCache == null) {
            return null;
        }
        
        // 使用迭代器而非 asMap()，减少内存占用
        return nativeCache.asMap().entrySet().iterator();
    }

    /**
     * 提取原生 Caffeine Cache
     * 通过反射获取 CaffeineCache 内部的原生 Caffeine Cache 实例
     * 
     * 注意：使用 protected 访问级别，便于测试时 mock 此方法
     */
    @SuppressWarnings("unchecked")
    protected com.github.benmanes.caffeine.cache.Cache<Object, Object> extractNativeCaffeineCache(CaffeineCache<Object, Object> jetCache) {
        try {
            Field f = jetCache.getClass().getDeclaredField("cache");
            f.setAccessible(true);
            Object nativeCache = f.get(jetCache);
            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                return (com.github.benmanes.caffeine.cache.Cache<Object, Object>) nativeCache;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // 反射失败，返回 null
        }
        return null;
    }
}
