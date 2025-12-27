package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.ProxyCache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 通过反射/解包获取 external cache 的 keyPrefix 信息。
 *
 * <p>用于将“物理 key（prefix + convertedKey）”规范化为“业务 key”，从而避免多次 get 试探。</p>
 */
public final class ExternalCacheKeyInspector {

    private ExternalCacheKeyInspector() {
    }

    public static Optional<String> resolveKeyPrefix(Cache<?, ?> cache) {
        Cache<?, ?> c = unwrap(cache);
        if (c == null) return Optional.empty();
        Object config;
        try {
            config = c.config();
        } catch (Throwable ignored) {
            return Optional.empty();
        }
        if (config == null) return Optional.empty();

        // ExternalCacheConfig#getKeyPrefix()
        Object prefix = invokeNoArg(config, "getKeyPrefix");
        if (prefix instanceof String) {
            String s = ((String) prefix).trim();
            if (!s.isEmpty()) return Optional.of(s);
        }

        // ExternalCacheConfig#getKeyPrefixSupplier()
        Object supplier = invokeNoArg(config, "getKeyPrefixSupplier");
        if (supplier instanceof Supplier) {
            try {
                Object v = ((Supplier<?>) supplier).get();
                if (v instanceof String) {
                    String s = ((String) v).trim();
                    if (!s.isEmpty()) return Optional.of(s);
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }

        // 兜底：直接读字段 keyPrefixSupplier
        Object field = readFieldQuietly(config, "keyPrefixSupplier");
        if (field instanceof Supplier) {
            try {
                Object v = ((Supplier<?>) field).get();
                if (v instanceof String) {
                    String s = ((String) v).trim();
                    if (!s.isEmpty()) return Optional.of(s);
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }

        return Optional.empty();
    }

    /**
     * 解包到更“具体”的 cache：
     * - ProxyCache: 取 target
     * - MultiLevelCache: 取最后一层（通常是远端层）
     */
    @SuppressWarnings({"rawtypes"})
    private static Cache<?, ?> unwrap(Cache<?, ?> cache) {
        Cache<?, ?> c = cache;
        int guard = 0;
        while (c instanceof ProxyCache && guard++ < 16) {
            try {
                c = ((ProxyCache) c).getTargetCache();
            } catch (Throwable t) {
                break;
            }
        }
        if (c instanceof MultiLevelCache) {
            try {
                Cache[] caches = ((MultiLevelCache) c).caches();
                if (caches != null && caches.length > 0) {
                    c = caches[caches.length - 1];
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }
        return c;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readFieldQuietly(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        Class<?> c = target.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}

