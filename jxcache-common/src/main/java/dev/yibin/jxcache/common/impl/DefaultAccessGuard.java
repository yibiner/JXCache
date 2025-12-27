package dev.yibin.jxcache.common.impl;

import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.common.spi.AccessGuard;

/**
 * 默认访问守卫实现（暂缓鉴权，默认允许）
 */
public class DefaultAccessGuard implements AccessGuard {

    @Override
    public boolean checkAccess(QueryRequest request, String clientInfo) {
        // 暂缓实现，默认允许访问
        return true;
    }
}
