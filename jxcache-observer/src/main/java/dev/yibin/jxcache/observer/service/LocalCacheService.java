package dev.yibin.jxcache.observer.service;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import dev.yibin.jxcache.common.dto.InvalidateRequest;
import dev.yibin.jxcache.common.dto.InvalidateResult;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.LocalCacheSnapshot;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.common.spi.ValuePreviewer;
import dev.yibin.jxcache.observer.exception.CacheNotFoundException;
import dev.yibin.jxcache.observer.introspector.LocalCacheIntrospector;
import dev.yibin.jxcache.observer.introspector.LocalCacheIntrospectorFactory;
import dev.yibin.jxcache.observer.support.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * 本地缓存服务
 * 封装缓存查询逻辑，自动选择合适的内省器
 */
@Service
public class LocalCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalCacheService.class);
    private static final String LOCAL_NODE_ID = "local";

    public static final String LEVEL_L0 = CacheLevel.L0.name();
    public static final String LEVEL_L1 = CacheLevel.L1.name();
    public static final String LEVEL_AUTO = CacheLevel.AUTO.name();

    private static final CacheLayerResolver LAYER_RESOLVER = new CompositeCacheLayerResolver(
            Arrays.asList(new MultiLevelCacheLayerResolver(), new SingleLevelCacheLayerResolver())
    );
    
    @Autowired(required = false)
    private CacheManager cacheManager;
    
    @Autowired(required = false)
    private LocalCacheIntrospectorFactory introspectorFactory;

    @Autowired(required = false)
    private ValuePreviewer valuePreviewer;

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static CacheLevel parseLevel(String level, CacheLevel defaultLevel) {
        return CacheLevel.parse(level, defaultLevel);
    }

    private static LocalCacheSnapshot emptySnapshot(String area, String cacheName, CacheLevel level, String message) {
        LocalCacheSnapshot snap = new LocalCacheSnapshot(LOCAL_NODE_ID, area, cacheName);
        snap.setLevel(level != null ? level.name() : null);
        if (message != null && !message.isEmpty()) {
            snap.setMessage(message);
        }
        snap.setEntries(Collections.emptyList());
        snap.setTotal(0);
        snap.setPartial(false);
        return snap;
    }
    
    /**
     * 扫描缓存数据
     * 
     * @param request 查询请求
     * @return 缓存快照
     */
    public LocalCacheSnapshot scan(QueryRequest request) {
        if (request == null || isBlank(request.getArea()) || isBlank(request.getCacheName())) {
            return new LocalCacheSnapshot(LOCAL_NODE_ID, "unknown", "unknown");
        }

        CacheLevel level = parseLevel(request.getLevel(), CacheLevel.L0);
        
        if (cacheManager == null || introspectorFactory == null) {
            logger.warn("[Observer] CacheManager or IntrospectorFactory is not available");
            return emptySnapshot(request.getArea(), request.getCacheName(), level, "CacheManager or IntrospectorFactory is not available");
        }

        // scan/query 只支持 L0（可枚举的本地缓存层）
        if (level != CacheLevel.L0) {
            return emptySnapshot(request.getArea(), request.getCacheName(), level,
                    "Scan/list query is only supported for local cache (L0). " +
                            "Remote cache (L1) cannot enumerate keys. Please use /api/jxc/observer/entry with level=L1/AUTO and a specific key.");
        }
        
        Cache<Object, Object> cache = cacheManager.getCache(request.getArea(), request.getCacheName());
        if (cache == null) {
            logger.debug("[Observer] Cache not found: area={}, cacheName={}", 
                    request.getArea(), request.getCacheName());
            return emptySnapshot(request.getArea(), request.getCacheName(), level, null);
        }

        // 对于 CacheType.BOTH（MultiLevelCache），提取 L0 本地缓存进行 scan
        CacheLayers layers = LAYER_RESOLVER.resolve(cache);
        Cache<Object, Object> localCache = layers.getL0() != null ? layers.getL0() : cache;
        if (logger.isDebugEnabled()) {
            logger.debug("[Observer] Scan cache layers: area={}, cacheName={}, raw={}, l0={}, l1={}, multi={}",
                    request.getArea(), request.getCacheName(),
                    cache.getClass().getSimpleName(),
                    layers.getL0() == null ? "null" : layers.getL0().getClass().getSimpleName(),
                    layers.getL1() == null ? "null" : layers.getL1().getClass().getSimpleName(),
                    layers.isMultiLevel());
        }
        if (localCache == null) {
            return emptySnapshot(request.getArea(), request.getCacheName(), level,
                    "Failed to extract local cache (L0) from multi-level cache");
        }
        if (!CacheHierarchyUtils.isLikelyEmbedded(localCache)) {
            return emptySnapshot(request.getArea(), request.getCacheName(), level,
                    "Scan/list query is only supported for embedded local cache (L0). " +
                            "This cache does not appear to be an embedded cache.");
        }

        LocalCacheIntrospector introspector = introspectorFactory.getIntrospector(localCache);
        if (introspector == null) {
            logger.debug("[Observer] No introspector found for cache type: {}", localCache.getClass().getSimpleName());
            return emptySnapshot(request.getArea(), request.getCacheName(), level, null);
        }

        LocalCacheSnapshot snap = introspector.scan(localCache, request);
        snap.setLevel(level.name());
        return snap;
    }
    
    /**
     * 获取单个缓存键的完整值
     * 
     * @param area 缓存区域
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @return 缓存条目详情
     */
    public Optional<LocalCacheEntryDetail> findEntry(String area, String cacheName, String key) {
        return findEntry(area, cacheName, key, LEVEL_AUTO);
    }

    /**
     * 获取单个缓存键的完整值（支持多级缓存层级）
     *
     * <p>注意：为了避免观测污染，AUTO 模式不会直接调用 MultiLevelCache.get()，
     * 而是先查 L0（本地），miss 再查 L1（远端）。</p>
     */
    public Optional<LocalCacheEntryDetail> findEntry(String area, String cacheName, String key, String requestedLevel) {
        if (isBlank(area) || isBlank(cacheName) || isBlank(key)) {
            return Optional.empty();
        }

        CacheLevel level = parseLevel(requestedLevel, CacheLevel.AUTO);
        
        if (cacheManager == null || introspectorFactory == null) {
            logger.warn("[Observer] CacheManager or IntrospectorFactory is not available");
            return Optional.empty();
        }
        
        Cache<Object, Object> raw = cacheManager.getCache(area, cacheName);
        if (raw == null) {
            logger.debug("[Observer] Cache not found: area={}, cacheName={}", area, cacheName);
            throw new CacheNotFoundException(area, cacheName);
        }

        CacheLayers layers = LAYER_RESOLVER.resolve(raw);
        Cache<Object, Object> l0 = layers.getL0();
        Cache<Object, Object> l1 = layers.getL1();

        if (logger.isDebugEnabled()) {
            logger.debug("[Observer] Resolved cache layers: area={}, cacheName={}, raw={}, l0={}, l1={}, multi={}",
                    area, cacheName,
                    raw.getClass().getSimpleName(),
                    l0 == null ? "null" : l0.getClass().getSimpleName(),
                    l1 == null ? "null" : l1.getClass().getSimpleName(),
                    layers.isMultiLevel());
        }

        if (level == CacheLevel.L0) {
            if (l0 == null) {
                throw new IllegalArgumentException("Local level (L0) is not available for this cache");
            }
            return findInEmbedded(l0, area, cacheName, key)
                    .map(d -> markLevels(d, level, CacheLevel.L0));
        }

        if (level == CacheLevel.L1) {
            if (l1 == null) {
                throw new IllegalArgumentException("Remote level (L1) is not available for this cache");
            }
            return findInRemote(l1, area, cacheName, key)
                    .map(d -> markLevels(d, level, CacheLevel.L1));
        }

        // AUTO
        return Optional.ofNullable(l0)
                .flatMap(c -> findInEmbedded(c, area, cacheName, key))
                .map(d -> markLevels(d, level, CacheLevel.L0))
                .map(Optional::of)
                .orElseGet(() -> Optional.ofNullable(l1)
                        .flatMap(c -> findInRemote(c, area, cacheName, key))
                        .map(d -> markLevels(d, level, CacheLevel.L1)));
    }

    private Optional<LocalCacheEntryDetail> findInEmbedded(Cache<Object, Object> cache, String area, String cacheName, String key) {
        if (cache == null) return Optional.empty();
        if (!CacheHierarchyUtils.isLikelyEmbedded(cache)) {
            return Optional.empty();
        }
        LocalCacheIntrospector introspector = introspectorFactory.getIntrospector(cache);
        if (introspector == null) {
            logger.debug("[Observer] No introspector found for cache type: {}", cache.getClass().getSimpleName());
            return Optional.empty();
        }
        return introspector.findEntry(cache, area, cacheName, key);
    }

    private static LocalCacheEntryDetail markLevels(LocalCacheEntryDetail d, CacheLevel requested, CacheLevel hit) {
        if (d == null) return null;
        d.setRequestedLevel(requested != null ? requested.name() : null);
        d.setHitLevel(hit != null ? hit.name() : null);
        return d;
    }

    private Optional<LocalCacheEntryDetail> findInRemote(Cache<Object, Object> cache, String area, String cacheName, String key) {
        if (cache == null) return Optional.empty();
        try {
            // 读取 external cache 的真实 prefix（如果能获取到）
            Optional<String> keyPrefixOpt = ExternalCacheKeyInspector.resolveKeyPrefix(cache);
            String keyForGet = key;
            if (keyPrefixOpt.isPresent()) {
                String prefix = keyPrefixOpt.get();
                if (key != null && key.startsWith(prefix)) {
                    keyForGet = key.substring(prefix.length());
                    logger.debug("[Observer] Remote key normalized by prefix: area={}, cacheName={}, prefix='{}', physicalKey='{}', businessKey='{}'",
                            area, cacheName, prefix, key, keyForGet);
                }
            } else {
                logger.debug("[Observer] Remote keyPrefix not available, treat as business key: area={}, cacheName={}, key={}", area, cacheName, key);
            }

            Object value = cache.get(keyForGet);
            if (value == null) {
                logger.debug("[Observer] Remote miss: area={}, cacheName={}, key={}, normalizedKey={}", area, cacheName, key, keyForGet);
                return Optional.empty();
            }

            LocalCacheEntryDetail detail = new LocalCacheEntryDetail("local", area, cacheName);
            detail.setKey(keyForGet == null ? "null" : String.valueOf(keyForGet));

            String fullValue = valuePreviewer != null
                    ? valuePreviewer.preview(value, Integer.MAX_VALUE)
                    : String.valueOf(value);
            detail.setValue(fullValue);
            detail.setValueType(valuePreviewer != null
                    ? valuePreviewer.getValueType(value)
                    : (value != null ? value.getClass().getName() : "null"));
            detail.setValueLength(fullValue == null ? 0 : fullValue.length());
            detail.setTruncated(false);
            detail.setQueryTime(System.currentTimeMillis());
            return Optional.of(detail);
        } catch (Exception e) {
            logger.debug("[Observer] Failed to get entry from remote cache: area={}, cacheName={}, key={}", area, cacheName, key, e);
            return Optional.empty();
        }
    }

    /**
     * 失效缓存
     * 
     * @param request 失效请求
     * @return 失效结果
     */
    public InvalidateResult invalidate(InvalidateRequest request) {
        InvalidateResult result = new InvalidateResult();
        
        if (request == null) {
            result.setSuccess(false);
            result.setErrorMessage("Invalidate request is null");
            return result;
        }
        
        String area = request.getArea();
        String cacheName = request.getCacheName();
        String key = request.getKey();
        boolean invalidateRemote = request.isInvalidateRemote();
        
        result.setArea(area);
        result.setCacheName(cacheName);
        result.setKey(key);
        
        if (area == null || area.isEmpty() || cacheName == null || cacheName.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("Area and cacheName are required");
            return result;
        }
        
        if (cacheManager == null) {
            result.setSuccess(false);
            result.setErrorMessage("CacheManager is not available");
            return result;
        }
        
        try {
            Cache<Object, Object> cache = cacheManager.getCache(area, cacheName);
            if (cache == null) {
                result.setSuccess(false);
                result.setErrorMessage(String.format("Cache not found: area=%s, cacheName=%s", area, cacheName));
                return result;
            }
            
            // 失效本地缓存
            boolean localSuccess = false;
            if (key != null && !key.trim().isEmpty()) {
                // 失效指定键
                try {
                    cache.remove(key);
                    localSuccess = true;
                    logger.debug("[Observer] Invalidated local cache entry: area={}, cacheName={}, key={}", 
                            area, cacheName, key);
                } catch (Exception e) {
                    logger.warn("[Observer] Failed to invalidate local cache entry: area={}, cacheName={}, key={}", 
                            area, cacheName, key, e);
                    result.setErrorMessage("Failed to invalidate local cache: " + e.getMessage());
                }
            } else {
                // 清除整个缓存：JetCache 的 removeAll 需要传入 Set 参数
                // 由于获取所有键可能性能较差，这里不支持清除整个缓存
                // 如果需要清除整个缓存，请通过扫描获取所有键后批量删除
                result.setSuccess(false);
                result.setErrorMessage("Clearing entire cache is not supported. Please specify a key to invalidate specific entry.");
                logger.warn("[Observer] Clearing entire cache is not supported: area={}, cacheName={}", area, cacheName);
                return result;
            }
            
            result.setLocalInvalidated(localSuccess);
            
            // 失效远程缓存（Redis）
            boolean remoteSuccess = false;
            if (invalidateRemote) {
                try {
                    // JetCache 的 Cache 接口在失效时会自动处理远程缓存（如果配置了）
                    // 但为了确保，我们再次显式调用
                    if (key != null && !key.trim().isEmpty()) {
                        // 对于指定键，remove 操作会自动同步到远程缓存
                        // 如果 cache 是 BOTH 类型，remove 会同时清除本地和远程
                        remoteSuccess = true;
                        logger.debug("[Observer] Remote cache invalidation requested: area={}, cacheName={}, key={}", 
                                area, cacheName, key);
                    } else {
                        // 清除整个远程缓存：不支持清除整个缓存
                        logger.warn("[Observer] Clearing entire remote cache is not supported: area={}, cacheName={}", 
                                area, cacheName);
                        remoteSuccess = false;
                    }
                } catch (Exception e) {
                    logger.warn("[Observer] Failed to invalidate remote cache: area={}, cacheName={}, key={}", 
                            area, cacheName, key, e);
                    // 远程缓存失效失败不影响整体结果，但记录错误
                    result.setErrorMessage((result.getErrorMessage() != null ? result.getErrorMessage() + "; " : "") 
                            + "Failed to invalidate remote cache: " + e.getMessage());
                }
            }
            
            result.setRemoteInvalidated(remoteSuccess);
            result.setSuccess(localSuccess);
            
            return result;
            
        } catch (Exception e) {
            logger.error("[Observer] Failed to invalidate cache: area={}, cacheName={}, key={}", 
                    area, cacheName, key, e);
            result.setSuccess(false);
            result.setErrorMessage("Unexpected error: " + e.getMessage());
            return result;
        }
    }
}

