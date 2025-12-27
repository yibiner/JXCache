package dev.yibin.jxcache.observer.introspector;

import com.alicp.jetcache.Cache;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.LocalCacheSnapshot;
import dev.yibin.jxcache.common.dto.QueryRequest;

import java.util.Optional;

/**
 * 本地缓存内省器接口
 * 提供统一的接口用于扫描和查询不同类型的本地缓存
 * 
 * 实现此接口以支持不同的本地缓存类型：
 * - CaffeineLocalCacheIntrospector: 支持 CaffeineCache
 * - LinkedHashMapLocalCacheIntrospector: 支持 LinkedHashMapCache
 * - 其他自定义实现
 */
public interface LocalCacheIntrospector {
    
    /**
     * 判断是否支持指定的缓存实例
     * 
     * @param cache JetCache Cache 实例
     * @return 是否支持
     */
    boolean supports(Cache<?, ?> cache);
    
    /**
     * 扫描缓存数据
     * 支持前缀过滤、分页、分片等功能
     * 
     * @param cache JetCache Cache 实例
     * @param request 查询请求
     * @return 缓存快照
     */
    LocalCacheSnapshot scan(Cache<Object, Object> cache, QueryRequest request);
    
    /**
     * 获取单个缓存键的完整值
     * 
     * @param cache JetCache Cache 实例
     * @param area 缓存区域
     * @param cacheName 缓存名称
     * @param key 缓存键（支持原始类型和字符串匹配）
     * @return 缓存条目详情
     */
    Optional<LocalCacheEntryDetail> findEntry(Cache<Object, Object> cache, 
                                               String area, 
                                               String cacheName, 
                                               String key);
}

