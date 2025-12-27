package dev.yibin.jxcache.tests;

import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.CacheType;
import org.springframework.stereotype.Service;

/**
 * Test service to trigger cache creation via @Cached annotation
 */
@Service
public class TestCacheService {
    @Cached(area = "test", name = "testCache", key = "#key", expire = 3600, cacheType = CacheType.LOCAL)
    public String initCache(String key) {
        return "init";
    }
}

