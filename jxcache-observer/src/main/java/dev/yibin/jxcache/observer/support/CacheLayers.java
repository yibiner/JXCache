package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.Cache;

/**
 * 解析后的缓存层级视图。
 */
public class CacheLayers {
    private final Cache<Object, Object> raw;
    private final Cache<Object, Object> l0;
    private final Cache<Object, Object> l1;
    private final boolean multiLevel;

    public CacheLayers(Cache<Object, Object> raw, Cache<Object, Object> l0, Cache<Object, Object> l1, boolean multiLevel) {
        this.raw = raw;
        this.l0 = l0;
        this.l1 = l1;
        this.multiLevel = multiLevel;
    }

    public Cache<Object, Object> getRaw() {
        return raw;
    }

    public Cache<Object, Object> getL0() {
        return l0;
    }

    public Cache<Object, Object> getL1() {
        return l1;
    }

    public boolean isMultiLevel() {
        return multiLevel;
    }
}

