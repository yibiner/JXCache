package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.Cache;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合解析器：按顺序选择第一个 supports 的策略。
 */
public class CompositeCacheLayerResolver implements CacheLayerResolver {
    private final List<CacheLayerResolver> delegates;

    public CompositeCacheLayerResolver(List<CacheLayerResolver> delegates) {
        this.delegates = delegates != null ? new ArrayList<>(delegates) : new ArrayList<>();
    }

    @Override
    public boolean supports(Cache<Object, Object> raw) {
        return raw != null;
    }

    @Override
    public CacheLayers resolve(Cache<Object, Object> raw) {
        for (CacheLayerResolver r : delegates) {
            try {
                if (r != null && r.supports(raw)) {
                    return r.resolve(raw);
                }
            } catch (Exception ignored) {
                // ignore and continue
            }
        }
        // 兜底
        return new SingleLevelCacheLayerResolver().resolve(raw);
    }
}

