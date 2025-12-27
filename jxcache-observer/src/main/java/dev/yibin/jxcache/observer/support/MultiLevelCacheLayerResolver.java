package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.Cache;

/**
 * MultiLevelCache 层级解析器。
 */
public class MultiLevelCacheLayerResolver implements CacheLayerResolver {

    @Override
    public boolean supports(Cache<Object, Object> raw) {
        return CacheHierarchyUtils.isLikelyMultiLevel(raw);
    }

    @Override
    public CacheLayers resolve(Cache<Object, Object> raw) {
        Cache<Object, Object> l0 = CacheHierarchyUtils.extractL0(raw);
        Cache<Object, Object> l1 = CacheHierarchyUtils.extractL1(raw);

        // 兼容某些 JetCache 版本/实现：内部层级顺序可能与预期相反
        if (l0 != null && l1 != null) {
            boolean l0Embedded = CacheHierarchyUtils.isLikelyEmbedded(l0);
            boolean l1Embedded = CacheHierarchyUtils.isLikelyEmbedded(l1);
            if (!l0Embedded && l1Embedded) {
                Cache<Object, Object> tmp = l0;
                l0 = l1;
                l1 = tmp;
            }
        }
        return new CacheLayers(raw, l0, l1, true);
    }
}

