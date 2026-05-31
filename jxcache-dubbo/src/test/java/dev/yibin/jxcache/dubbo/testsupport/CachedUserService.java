package dev.yibin.jxcache.dubbo.testsupport;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;

/**
 * 用于验证 Dubbo consumer 侧“先缓存、后 RPC”调用路径的测试接口。
 */
public interface CachedUserService {

    /**
     * consumer 侧优先命中本地缓存；未命中时回退到 no-cache RPC 方法。
     *
     * @param userId 用户标识
     * @return 用户信息
     */
    @Cached(name = "dubboUserCache", key = "#userId", expire = 60, cacheType = CacheType.LOCAL)
    default String getUser(String userId) {
        return getUserNoCache(userId);
    }

    /**
     * 模拟真实 Dubbo RPC 调用的方法。
     *
     * @param userId 用户标识
     * @return 用户信息
     */
    String getUserNoCache(String userId);

    /**
     * 普通方法，不参与缓存拦截。
     *
     * @param userId 用户标识
     * @return 方法返回值
     */
    String ping(String userId);
}
