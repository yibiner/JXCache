package dev.yibin.jxcache.observer.introspector;

import com.alicp.jetcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 本地缓存内省器工厂
 * 根据缓存类型自动选择合适的内省器实现
 * 
 * 支持插件化扩展：
 * - 所有实现了 LocalCacheIntrospector 接口的 Bean 都会被自动注入
 * - 通过 supports() 方法判断是否支持指定的缓存类型
 * - 按注入顺序查找第一个支持的内省器
 */
@Component
public class LocalCacheIntrospectorFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalCacheIntrospectorFactory.class);
    
    private final List<LocalCacheIntrospector> introspectors;
    
    /**
     * 构造函数，自动注入所有 LocalCacheIntrospector 实现
     */
    @Autowired(required = false)
    public LocalCacheIntrospectorFactory(List<LocalCacheIntrospector> introspectors) {
        this.introspectors = introspectors != null ? new ArrayList<>(introspectors) : new ArrayList<>();
        logger.info("[Observer] LocalCacheIntrospectorFactory initialized with {} introspector(s): {}", 
                this.introspectors.size(), 
                this.introspectors.stream()
                        .map(i -> i.getClass().getSimpleName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("none"));
    }
    
    /**
     * 根据缓存实例获取合适的内省器
     * 
     * @param cache JetCache Cache 实例
     * @return 支持该缓存类型的内省器，如果不存在则返回 null
     */
    public LocalCacheIntrospector getIntrospector(Cache<?, ?> cache) {
        if (cache == null) {
            return null;
        }
        
        for (LocalCacheIntrospector introspector : introspectors) {
            try {
                if (introspector.supports(cache)) {
                    logger.debug("[Observer] Selected introspector: {} for cache type: {}", 
                            introspector.getClass().getSimpleName(), 
                            cache.getClass().getSimpleName());
                    return introspector;
                }
            } catch (Exception e) {
                logger.warn("[Observer] Error checking support in introspector: {}", 
                        introspector.getClass().getSimpleName(), e);
            }
        }
        
        logger.debug("[Observer] No introspector found for cache type: {}", cache.getClass().getSimpleName());
        return null;
    }
    
    /**
     * 检查是否有内省器支持指定的缓存类型
     * 
     * @param cache JetCache Cache 实例
     * @return 是否支持
     */
    public boolean isSupported(Cache<?, ?> cache) {
        return getIntrospector(cache) != null;
    }
    
    /**
     * 获取所有已注册的内省器
     * 
     * @return 内省器列表
     */
    public List<LocalCacheIntrospector> getAllIntrospectors() {
        return new ArrayList<>(introspectors);
    }
}

