package dev.yibin.jxcache.observer.introspector;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheValueHolder;
import dev.yibin.jxcache.common.dto.CacheEntry;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.LocalCacheSnapshot;
import dev.yibin.jxcache.common.dto.PageRequest;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.common.spi.ValuePreviewer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 本地缓存内省器抽象基类
 * 提供通用的实现逻辑，子类只需实现特定缓存类型的遍历方法
 */
public abstract class AbstractLocalCacheIntrospector implements LocalCacheIntrospector {
    
    protected static final String LOCAL_NODE_ID = "local";
    
    @Autowired(required = false)
    protected ValuePreviewer valuePreviewer;
    
    protected static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    
    @Override
    public LocalCacheSnapshot scan(Cache<Object, Object> cache, QueryRequest request) {
        if (request == null || isBlank(request.getArea()) || isBlank(request.getCacheName())) {
            return new LocalCacheSnapshot(LOCAL_NODE_ID, "unknown", "unknown");
        }
        
        if (cache == null) {
            return new LocalCacheSnapshot(LOCAL_NODE_ID, request.getArea(), request.getCacheName());
        }
        
        // 获取所有条目（由子类实现特定缓存类型的遍历）
        Iterator<Map.Entry<Object, Object>> entryIterator = getEntryIterator(cache);
        if (entryIterator == null || !entryIterator.hasNext()) {
            LocalCacheSnapshot snapshot = new LocalCacheSnapshot(LOCAL_NODE_ID, request.getArea(), request.getCacheName());
            snapshot.setEntries(Collections.emptyList());
            snapshot.setTotal(0);
            snapshot.setPartial(false);
            return snapshot;
        }
        
        // 转换为列表并应用过滤
        List<Map.Entry<Object, Object>> allEntries = new ArrayList<>();
        entryIterator.forEachRemaining(allEntries::add);
        
        // 前缀过滤
        List<Map.Entry<Object, Object>> filtered = allEntries.stream()
                .filter(e -> {
                    String prefix = request.getKeyPrefix();
                    if (isBlank(prefix)) return true;
                    Object k = e.getKey();
                    return k != null && String.valueOf(k).startsWith(prefix);
                })
                .collect(Collectors.toList());
        
        // 排序
        filtered.sort(Comparator.comparing(e -> String.valueOf(e.getKey()), Comparator.nullsFirst(String::compareTo)));
        
        // 分片
        List<Map.Entry<Object, Object>> sharded = applySharding(filtered, request);
        
        // 分页
        PageRequest page = request.getPageRequest() != null ? request.getPageRequest() : new PageRequest(1, 20);
        int offset = page.getOffset();
        int limit = Math.max(page.getPageSize(), 1);
        
        List<Map.Entry<Object, Object>> pageEntries = sharded.stream()
                .skip(Math.max(offset, 0))
                .limit(limit)
                .collect(Collectors.toList());
        
        // 构建结果
        List<CacheEntry> entries = pageEntries.stream()
                .map(e -> {
                    CacheEntry ce = new CacheEntry();
                    ce.setKey(e.getKey() == null ? "null" : String.valueOf(e.getKey()));
                    Object val = e.getValue();
                    long accessTime = 0;
                    long expireTime = 0;
                    // JetCache embedded 的原生 Map 中通常存的是 CacheValueHolder，而不是业务 value
                    if (val instanceof CacheValueHolder) {
                        CacheValueHolder<?> holder = (CacheValueHolder<?>) val;
                        accessTime = holder.getAccessTime();
                        expireTime = holder.getExpireTime();
                        val = holder.getValue();
                    }
                    if (valuePreviewer != null) {
                        ce.setValuePreview(valuePreviewer.preview(val, 300));
                        ce.setValueType(valuePreviewer.getValueType(val));
                    } else {
                        ce.setValuePreview(val != null ? val.toString() : "null");
                        ce.setValueType(val != null ? val.getClass().getName() : "null");
                    }
                    // CacheValueHolder 没有 createTime，仅有 accessTime；这里用 accessTime 同步填充
                    if (accessTime > 0) {
                        ce.setCreateTime(accessTime);
                        ce.setLastAccessTime(accessTime);
                    } else {
                        long now = System.currentTimeMillis();
                        ce.setCreateTime(now);
                        ce.setLastAccessTime(now);
                    }
                    ce.setExpireTime(expireTime);
                    return ce;
                })
                .collect(Collectors.toList());
        
        LocalCacheSnapshot snapshot = new LocalCacheSnapshot(LOCAL_NODE_ID, request.getArea(), request.getCacheName());
        snapshot.setEntries(entries);
        snapshot.setTotal(sharded.size());
        snapshot.setPartial(request.getTotalShards() > 1);
        return snapshot;
    }
    
    @Override
    public Optional<LocalCacheEntryDetail> findEntry(Cache<Object, Object> cache, 
                                                      String area, 
                                                      String cacheName, 
                                                      String key) {
        if (isBlank(area) || isBlank(cacheName) || key == null || key.trim().isEmpty()) {
            return Optional.empty();
        }
        
        if (cache == null) {
            return Optional.empty();
        }
        
        // 优化：优先使用 Cache.get() 方法，O(1) 时间复杂度
        Object value = cache.get(key);
        Object actualKey = key;
        
        // 如果直接获取失败，尝试字符串匹配（兼容性处理）
        if (value == null) {
            Optional<Map.Entry<Object, Object>> match = findEntryByStringMatch(cache, key);
            if (match.isPresent()) {
                Map.Entry<Object, Object> entry = match.get();
                actualKey = entry.getKey();
                value = entry.getValue();
            } else {
                return Optional.empty();
            }
        }
        
        // 构建结果
        LocalCacheEntryDetail detail = new LocalCacheEntryDetail(LOCAL_NODE_ID, area, cacheName);
        detail.setKey(actualKey == null ? "null" : String.valueOf(actualKey));
        
        String fullValue = valuePreviewer != null 
                ? valuePreviewer.preview(value, Integer.MAX_VALUE)
                : (value != null ? value.toString() : "null");
        detail.setValue(fullValue);
        detail.setValueType(valuePreviewer != null 
                ? valuePreviewer.getValueType(value)
                : (value != null ? value.getClass().getName() : "null"));
        detail.setValueLength(fullValue == null ? 0 : fullValue.length());
        detail.setTruncated(false);
        detail.setQueryTime(System.currentTimeMillis());
        return Optional.of(detail);
    }
    
    /**
     * 获取缓存条目的迭代器
     * 子类需要实现此方法，提供特定缓存类型的遍历方式
     * 
     * @param cache JetCache Cache 实例
     * @return 条目迭代器，如果无法遍历则返回 null
     */
    protected abstract Iterator<Map.Entry<Object, Object>> getEntryIterator(Cache<Object, Object> cache);
    
    /**
     * 通过字符串匹配查找缓存条目（降级方案）
     * 仅在 Cache.get() 直接获取失败时使用
     * 
     * @param cache JetCache Cache 实例
     * @param key 缓存键（字符串）
     * @return 匹配的缓存条目
     */
    protected Optional<Map.Entry<Object, Object>> findEntryByStringMatch(Cache<Object, Object> cache, String key) {
        Iterator<Map.Entry<Object, Object>> iterator = getEntryIterator(cache);
        if (iterator == null) {
            return Optional.empty();
        }
        
        while (iterator.hasNext()) {
            Map.Entry<Object, Object> entry = iterator.next();
            Object actualKey = entry.getKey();
            if (Objects.equals(actualKey, key)) {
                return Optional.of(entry);
            }
            if (actualKey != null && key.equals(String.valueOf(actualKey))) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
    
    /**
     * 应用分片逻辑
     */
    protected List<Map.Entry<Object, Object>> applySharding(List<Map.Entry<Object, Object>> entries, QueryRequest request) {
        if (entries == null || entries.isEmpty() || request == null || request.getTotalShards() <= 1) {
            return entries != null ? entries : Collections.emptyList();
        }
        int shard = request.getShard();
        int total = request.getTotalShards();
        if (shard < 0 || shard >= total) {
            return Collections.emptyList();
        }
        List<Map.Entry<Object, Object>> out = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (i % total == shard) {
                out.add(entries.get(i));
            }
        }
        return out;
    }
}

