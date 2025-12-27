package dev.yibin.jxcache.common.impl;

import dev.yibin.jxcache.common.spi.RateLimiter;

/**
 * 默认限流器实现（暂缓限流，默认允许）
 */
public class DefaultRateLimiter implements RateLimiter {

    @Override
    public boolean tryAcquire(String key) {
        // 暂缓实现，默认允许通过
        return true;
    }

    @Override
    public long getRemainingQuota(String key) {
        // 暂缓实现，返回无限配额
        return Long.MAX_VALUE;
    }
}
