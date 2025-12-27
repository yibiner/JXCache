package dev.yibin.jxcache.common.impl;

import dev.yibin.jxcache.common.dto.QueryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultAccessGuard 单元测试
 */
class DefaultAccessGuardTest {
    
    private DefaultAccessGuard accessGuard;
    
    @BeforeEach
    void setUp() {
        accessGuard = new DefaultAccessGuard();
    }
    
    @Test
    void testCheckAccessWithValidRequest() {
        QueryRequest request = new QueryRequest();
        request.setArea("test");
        request.setCacheName("testCache");
        String clientInfo = "127.0.0.1";
        boolean result = accessGuard.checkAccess(request, clientInfo);
        assertThat(result).isTrue();
    }
    
    @Test
    void testCheckAccessWithNullRequest() {
        boolean result = accessGuard.checkAccess(null, "127.0.0.1");
        assertThat(result).isTrue();
    }
    
    @Test
    void testCheckAccessWithNullClientInfo() {
        QueryRequest request = new QueryRequest();
        request.setArea("test");
        request.setCacheName("testCache");
        boolean result = accessGuard.checkAccess(request, null);
        assertThat(result).isTrue();
    }
}
