package dev.yibin.jxcache.observer.support;

/**
 * Observer 查询层级。
 */
public enum CacheLevel {
    L0,
    L1,
    AUTO;

    public static CacheLevel parse(String level, CacheLevel defaultLevel) {
        if (level == null) return defaultLevel;
        String s = level.trim();
        if (s.isEmpty()) return defaultLevel;
        try {
            return CacheLevel.valueOf(s.toUpperCase());
        } catch (Exception ignored) {
            return defaultLevel;
        }
    }
}

