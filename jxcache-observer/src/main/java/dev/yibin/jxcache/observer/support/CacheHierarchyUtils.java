package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JetCache 多级缓存（MultiLevelCache）层级提取工具。
 *
 * <p>目标：在不依赖具体实现类（避免编译期强耦合）的情况下，通过反射从 MultiLevelCache 中提取 L0/L1。</p>
 *
 * <p>重要语义：本工具只用于“取出某一层的 Cache 实例”，不会调用 MultiLevelCache.get()，
 * 以避免触发远端读取导致回填本地（观测污染）。</p>
 */
public final class CacheHierarchyUtils {
    private static final Logger logger = LoggerFactory.getLogger(CacheHierarchyUtils.class);

    private CacheHierarchyUtils() {
    }

    public static boolean isLikelyMultiLevel(Cache<?, ?> cache) {
        if (cache == null) return false;
        String n = cache.getClass().getName();
        return n.contains("MultiLevelCache");
    }

    /**
     * 判断是否是“可枚举”的本地缓存实现（embedded），用于 scan/query。
     */
    public static boolean isLikelyEmbedded(Cache<?, ?> cache) {
        if (cache == null) return false;
        String n = cache.getClass().getName();
        // JetCache embedded 包下的本地缓存实现（CaffeineCache / LinkedHashMapCache 等）
        return n.contains(".embedded.");
    }

    /**
     * 提取 L0（本地缓存）实例。
     *
     * @return L0 cache；如果无法提取则返回 null
     */
    public static Cache<Object, Object> extractL0(Cache<?, ?> cache) {
        List<Cache<Object, Object>> levels = extractLevels(cache);
        if (levels.isEmpty()) return null;

        // 优先返回 embedded（可枚举）的本地缓存层，避免不同 JetCache 版本/实现顺序差异导致层级反转
        for (Cache<Object, Object> c : levels) {
            if (isLikelyEmbedded(c)) {
                return c;
            }
        }
        // 兜底：保持旧行为（假设 index=0 是 L0）
        return levels.get(0);
    }

    /**
     * 提取 L1（远端缓存）实例。
     *
     * @return L1 cache；如果不存在则返回 null
     */
    public static Cache<Object, Object> extractL1(Cache<?, ?> cache) {
        List<Cache<Object, Object>> levels = extractLevels(cache);
        if (levels.size() < 2) return null;

        // 优先返回非 embedded 的层作为远端层（L1）
        for (Cache<Object, Object> c : levels) {
            if (!isLikelyEmbedded(c)) {
                return c;
            }
        }

        // 兜底：保持旧行为（假设 index=1 是 L1）
        return levels.get(1);
    }

    /**
     * 尝试提取多级缓存的各级 Cache（按层级顺序）。
     * <p>对于非 MultiLevelCache，会返回 empty；调用方可自行决定 fallback。</p>
     */
    @SuppressWarnings("unchecked")
    public static List<Cache<Object, Object>> extractLevels(Cache<?, ?> cache) {
        if (cache == null) return Collections.emptyList();

        // 快速路径：字段名常见为 "caches"
        Object maybe = readFieldQuietly(cache, "caches");
        List<Cache<Object, Object>> out = toCacheList(maybe);
        if (!out.isEmpty()) return out;

        // 兼容路径：尝试 local/remote 字段
        Object local = readFieldQuietly(cache, "localCache");
        Object remote = readFieldQuietly(cache, "remoteCache");
        Cache<Object, Object> lc = (local instanceof Cache) ? (Cache<Object, Object>) local : null;
        Cache<Object, Object> rc = (remote instanceof Cache) ? (Cache<Object, Object>) remote : null;
        if (lc != null || rc != null) {
            List<Cache<Object, Object>> res = new ArrayList<>(2);
            if (lc != null) res.add(lc);
            if (rc != null) res.add(rc);
            return res;
        }

        // 兜底：遍历字段，找 Cache[] / List<Cache>
        return scanFieldsForLevels(cache);
    }

    private static Object readFieldQuietly(Object target, String fieldName) {
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            } catch (Throwable t) {
                logger.debug("[Observer] Failed reading field {} on {}: {}", fieldName, target.getClass().getName(), t.toString());
                return null;
            }
        }
        return null;
    }

    private static List<Cache<Object, Object>> scanFieldsForLevels(Cache<?, ?> cache) {
        try {
            Class<?> c = cache.getClass();
            while (c != null && c != Object.class) {
                Field[] fields = c.getDeclaredFields();
                for (Field f : fields) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(cache);
                        List<Cache<Object, Object>> list = toCacheList(v);
                        if (!list.isEmpty()) {
                            return list;
                        }
                    } catch (Throwable ignored) {
                        // ignore
                    }
                }
                c = c.getSuperclass();
            }
            return Collections.emptyList();
        } catch (Throwable t) {
            logger.debug("[Observer] Failed scanning fields for cache levels: {}", t.toString());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Cache<Object, Object>> toCacheList(Object maybe) {
        if (maybe == null) return Collections.emptyList();

        if (maybe instanceof Cache[]) {
            Cache<?, ?>[] arr = (Cache<?, ?>[]) maybe;
            List<Cache<Object, Object>> res = new ArrayList<>(arr.length);
            for (Cache<?, ?> c : arr) {
                if (c != null) res.add((Cache<Object, Object>) c);
            }
            return res;
        }

        if (maybe instanceof Object[]) {
            Object[] arr = (Object[]) maybe;
            List<Cache<Object, Object>> res = new ArrayList<>(arr.length);
            for (Object o : arr) {
                if (o instanceof Cache) res.add((Cache<Object, Object>) o);
            }
            return res;
        }

        if (maybe instanceof List) {
            List<?> list = (List<?>) maybe;
            List<Cache<Object, Object>> res = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o instanceof Cache) res.add((Cache<Object, Object>) o);
            }
            return res;
        }

        return Collections.emptyList();
    }
}
