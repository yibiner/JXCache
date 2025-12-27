package dev.yibin.jxcache.common.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultRateLimiter 单元测试
 */
class DefaultRateLimiterTest {
    
    private DefaultRateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new DefaultRateLimiter();
    }
    
    @Test
    void testTryAcquireWithValidKey() {
        boolean result = rateLimiter.tryAcquire("test-key");
        assertThat(result).isTrue();
    }
    
    @Test
    void testTryAcquireWithNullKey() {
        boolean result = rateLimiter.tryAcquire(null);
        assertThat(result).isTrue();
    }
    
    @Test
    void testTryAcquireWithEmptyKey() {
        boolean result = rateLimiter.tryAcquire("");
        assertThat(result).isTrue();
    }
    
    @Test
    void testGetRemainingQuotaWithValidKey() {
        long result = rateLimiter.getRemainingQuota("test-key");
        assertThat(result).isEqualTo(Long.MAX_VALUE);
    }
    
    @Test
    void testGetRemainingQuotaWithNullKey() {
        long result = rateLimiter.getRemainingQuota(null);
        assertThat(result).isEqualTo(Long.MAX_VALUE);
    }
}
