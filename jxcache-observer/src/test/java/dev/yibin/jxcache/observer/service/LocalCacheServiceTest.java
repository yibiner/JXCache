package dev.yibin.jxcache.observer.service;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import dev.yibin.jxcache.common.dto.InvalidateRequest;
import dev.yibin.jxcache.common.dto.InvalidateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LocalCacheService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class LocalCacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache<Object, Object> cache;

    @InjectMocks
    private LocalCacheService localCacheService;

    @Test
    void testInvalidateWithNullRequest() {
        InvalidateResult result = localCacheService.invalidate(null);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalidate request is null");
    }

    @Test
    void testInvalidateWithNullArea() {
        InvalidateRequest request = new InvalidateRequest();
        request.setCacheName("test-cache");
        request.setKey("test-key");
        InvalidateResult result = localCacheService.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Area and cacheName are required");
    }

    @Test
    void testInvalidateWithNullCacheName() {
        InvalidateRequest request = new InvalidateRequest();
        request.setArea("test-area");
        request.setKey("test-key");
        InvalidateResult result = localCacheService.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Area and cacheName are required");
    }

    @Test
    void testInvalidateWithNullCacheManager() {
        LocalCacheService service = new LocalCacheService();
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", "test-key");
        InvalidateResult result = service.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("CacheManager is not available");
    }

    @Test
    void testInvalidateWithCacheNotFound() {
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", "test-key");
        when(cacheManager.getCache("test-area", "test-cache")).thenReturn(null);
        InvalidateResult result = localCacheService.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Cache not found");
    }

    @Test
    void testInvalidateWithKey() {
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", "test-key");
        request.setInvalidateRemote(false);
        when(cacheManager.getCache("test-area", "test-cache")).thenReturn(cache);
        // JetCache Cache.remove() 方法返回 CacheResult，这里 mock 返回 null 表示成功
        // 使用 doAnswer 来避免类型检查问题
        doAnswer(invocation -> null).when(cache).remove("test-key");
        InvalidateResult result = localCacheService.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLocalInvalidated()).isTrue();
        assertThat(result.isRemoteInvalidated()).isFalse();
        verify(cache, times(1)).remove("test-key");
    }

    @Test
    void testInvalidateWithKeyAndRemote() {
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", "test-key");
        request.setInvalidateRemote(true);
        when(cacheManager.getCache("test-area", "test-cache")).thenReturn(cache);
        // JetCache Cache.remove() 方法返回 CacheResult，这里 mock 返回 null 表示成功
        // 使用 doAnswer 来避免类型检查问题
        doAnswer(invocation -> null).when(cache).remove("test-key");
        InvalidateResult result = localCacheService.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLocalInvalidated()).isTrue();
        assertThat(result.isRemoteInvalidated()).isTrue();
        verify(cache, times(1)).remove("test-key");
    }

    @Test
    void testInvalidateWithoutKey() {
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", null);
        when(cacheManager.getCache("test-area", "test-cache")).thenReturn(cache);
        InvalidateResult result = localCacheService.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Clearing entire cache is not supported");
        verify(cache, never()).remove(any());
        verify(cache, never()).removeAll(any());
    }

    @Test
    void testInvalidateWithException() {
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", "test-key");
        when(cacheManager.getCache("test-area", "test-cache")).thenReturn(cache);
        // 使用 doThrow 来模拟异常
        doThrow(new RuntimeException("Cache remove failed")).when(cache).remove("test-key");
        InvalidateResult result = localCacheService.invalidate(request);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Failed to invalidate local cache");
    }
}

