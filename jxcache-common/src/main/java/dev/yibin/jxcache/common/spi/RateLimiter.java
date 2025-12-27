package dev.yibin.jxcache.common.spi;

/**
 * 限流器 - 用于限流（暂缓实现）
 * @author Yibin
 * @since 1.0.0
 */
public interface RateLimiter {
    
    /**
     * 检查是否允许通过
     * 
     * @param key 限流键
     * @return 是否允许通过
     */
    boolean tryAcquire(String key);
    
    /**
     * 获取剩余配额
     * 
     * @param key 限流键
     * @return 剩余配额
     */
    long getRemainingQuota(String key);
}
