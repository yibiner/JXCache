package dev.yibin.jxcache.observer.support;

import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JetCache 缓存区域注册表
 * 从配置文件和运行时环境聚合 JetCache 的缓存区域信息
 */
@Component
public class JetCacheAreaRegistry {
    private static final String PREFIX_LOCAL = "local_";
    private static final String PREFIX_REMOTE = "remote_";
    private static final Logger logger = LoggerFactory.getLogger(JetCacheAreaRegistry.class);

    private final Environment env;
    private final GlobalCacheConfig global;
    private final AtomicReference<Set<String>> areas = new AtomicReference<>(Collections.emptySet());
    private final AtomicReference<Set<String>> rawAreas = new AtomicReference<>(Collections.emptySet());

    /**
     * 构造函数
     * <p>
     * GlobalCacheConfig 是可选的，如果不存在（例如测试环境），将从配置文件读取缓存区域信息
     *
     * @param env Spring Environment
     * @param global GlobalCacheConfig（可选，可能为 null）
     */
    public JetCacheAreaRegistry(Environment env, 
                                @org.springframework.beans.factory.annotation.Autowired(required = false) GlobalCacheConfig global) {
        this.env = Objects.requireNonNull(env, "env");
        this.global = global;
    }

    private static <K, V> Map<K, V> safeMap(Map<K, V> in) {
        return in == null ? Collections.emptyMap() : in;
    }

    private static void addKeysWithPrefix(Set<String> prefixedOut, Set<String> rawOut, Collection<String> keys, String prefix) {
        if (keys == null) return;
        for (String k : keys) {
            if (k == null) continue;
            String s = k.trim();
            if (s.isEmpty()) continue;
            prefixedOut.add(prefix + s);
            rawOut.add(s);
        }
    }

    @PostConstruct
    public void init() {
        recompute();
    }

    /**
     * 应用启动完成后重新计算缓存区域
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.debug("[Observer] ApplicationReadyEvent received, recomputing JetCache areas");
        recompute();
    }

    /**
     * 获取带前缀的缓存区域列表
     */
    public Set<String> listAreas() {
        return areas.get();
    }

    /**
     * 获取原始缓存区域列表（不带前缀）
     */
    public Set<String> listAreasRaw() {
        return rawAreas.get();
    }

    /**
     * 重新计算缓存区域
     */
    public void recompute() {
        internalRecompute();
    }

    private synchronized void internalRecompute() {
        final Set<String> prefixed = new LinkedHashSet<>(16);
        final Set<String> raw = new LinkedHashSet<>(16);

        Map<String, Object> localMap = bindMap("jetcache.local");
        Map<String, Object> remoteMap = bindMap("jetcache.remote");
        addKeysWithPrefix(prefixed, raw, localMap.keySet(), PREFIX_LOCAL);
        addKeysWithPrefix(prefixed, raw, remoteMap.keySet(), PREFIX_REMOTE);

        if (global != null) {
            Map<String, ?> locals = safeMap(global.getLocalCacheBuilders());
            Map<String, ?> remotes = safeMap(global.getRemoteCacheBuilders());
            addKeysWithPrefix(prefixed, raw, locals.keySet(), PREFIX_LOCAL);
            addKeysWithPrefix(prefixed, raw, remotes.keySet(), PREFIX_REMOTE);
        } else {
            logger.debug("[Observer] GlobalCacheConfig is null, skip runtime builders");
        }

        Set<String> newPrefixed = Collections.unmodifiableSet(prefixed);
        Set<String> newRaw = Collections.unmodifiableSet(raw);

        Set<String> oldPrefixed = areas.get();
        Set<String> oldRaw = rawAreas.get();

        boolean changed = !Objects.equals(oldPrefixed, newPrefixed) || !Objects.equals(oldRaw, newRaw);
        if (changed) {
            areas.set(newPrefixed);
            rawAreas.set(newRaw);
            logger.info("[Observer] JetCache areas updated, prefixed: {} -> {}, raw: {} -> {}",
                    oldPrefixed, newPrefixed, oldRaw, newRaw);
        } else {
            logger.debug("[Observer] JetCache areas unchanged, prefixed: {}, raw: {}", newPrefixed, newRaw);
        }
    }

    /**
     * 使用 Binder 绑定配置前缀到 Map
     */
    private Map<String, Object> bindMap(String prefix) {
        try {
            return Binder.get(env).bind(prefix, Bindable.mapOf(String.class, Object.class))
                    .orElseGet(Collections::emptyMap);
        } catch (Exception e) {
            logger.debug("[Observer] Failed to bind prefix '{}': {}", prefix, e.toString());
            return Collections.emptyMap();
        }
    }
}
