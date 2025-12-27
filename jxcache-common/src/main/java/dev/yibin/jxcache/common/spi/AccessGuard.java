package dev.yibin.jxcache.common.spi;

import dev.yibin.jxcache.common.dto.QueryRequest;

/**
 * 访问守卫 - 用于鉴权（暂缓实现）
 * @author Yibin
 * @since 1.0.0
 */
public interface AccessGuard {
    
    /**
     * 检查访问权限
     * 
     * @param request 查询请求
     * @param clientInfo 客户端信息
     * @return 是否允许访问
     */
    boolean checkAccess(QueryRequest request, String clientInfo);
}
