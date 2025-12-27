package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.Cache;

/**
 * 单层缓存解析器（embedded/external）。
 */
public class SingleLevelCacheLayerResolver implements CacheLayerResolver {

    @Override
    public boolean supports(Cache<Object, Object> raw) {
        // 兜底：任何 Cache 都可用
        return raw != null;
    }

    @Override
    public CacheLayers resolve(Cache<Object, Object> raw) {
        if (raw == null) return new CacheLayers(null, null, null, false);
        boolean embedded = CacheHierarchyUtils.isLikelyEmbedded(raw);
        Cache<Object, Object> l0 = embedded ? raw : null;
        Cache<Object, Object> l1 = embedded ? null : raw;
        return new CacheLayers(raw, l0, l1, false);
    }
}

