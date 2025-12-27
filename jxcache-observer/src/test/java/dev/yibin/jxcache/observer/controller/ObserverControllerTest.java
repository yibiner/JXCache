package dev.yibin.jxcache.observer.controller;

import dev.yibin.jxcache.common.dto.InvalidateRequest;
import dev.yibin.jxcache.common.dto.InvalidateResult;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.LocalCacheSnapshot;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.observer.service.LocalCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * LocalCacheController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ObserverControllerTest {

    @Mock
    private LocalCacheService localCacheService;

    @InjectMocks
    private ObserverController controller;


    @Test
    void testQueryGet() {
        LocalCacheSnapshot expectedSnapshot = new LocalCacheSnapshot("local", "test-area", "test-cache");
        QueryRequest expectedRequest = new QueryRequest();
        expectedRequest.setArea("test-area");
        expectedRequest.setCacheName("test-cache");
        expectedRequest.setLevel(null);
        expectedRequest.setKeyPrefix("test-prefix");
        expectedRequest.setPageRequest(new dev.yibin.jxcache.common.dto.PageRequest(1, 20));
        expectedRequest.setShard(0);
        expectedRequest.setTotalShards(1);

        when(localCacheService.scan(expectedRequest)).thenReturn(expectedSnapshot);
        LocalCacheSnapshot result = controller.queryGet("test-area", "test-cache", null, "test-prefix", 1, 20, 0, 1);
        assertThat(result).isEqualTo(expectedSnapshot);
    }

    @Test
    void testQueryPost() {
        QueryRequest request = new QueryRequest();
        request.setArea("test-area");
        request.setCacheName("test-cache");

        LocalCacheSnapshot expectedSnapshot = new LocalCacheSnapshot("local", "test-area", "test-cache");
        when(localCacheService.scan(request)).thenReturn(expectedSnapshot);
        LocalCacheSnapshot result = controller.queryPost(request);
        assertThat(result).isEqualTo(expectedSnapshot);
    }

    @Test
    void testGetEntry() {
        LocalCacheEntryDetail detail = new LocalCacheEntryDetail("local", "test-area", "test-cache");
        detail.setKey("123");
        detail.setValue("value123");
        when(localCacheService.findEntry("test-area", "test-cache", "123", "AUTO"))
                .thenReturn(Optional.of(detail));

        LocalCacheEntryDetail result = controller.getEntry("test-area", "test-cache", "123", "AUTO");

        assertThat(result).isEqualTo(detail);
    }

    @Test
    void testGetEntryNotFound() {
        when(localCacheService.findEntry("test-area", "test-cache", "missing", "AUTO"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getEntry("test-area", "test-cache", "missing", "AUTO"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }

    @Test
    void testInvalidate() {
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";
        boolean invalidateRemote = false;

        InvalidateResult expectedResult = new InvalidateResult(area, cacheName, key);
        expectedResult.setSuccess(true);
        expectedResult.setLocalInvalidated(true);
        expectedResult.setRemoteInvalidated(false);

        InvalidateRequest expectedRequest = new InvalidateRequest(area, cacheName, key);
        expectedRequest.setInvalidateRemote(invalidateRemote);
        when(localCacheService.invalidate(expectedRequest)).thenReturn(expectedResult);
        InvalidateResult result = controller.invalidate(area, cacheName, key, invalidateRemote);
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isLocalInvalidated()).isTrue();
        assertThat(result.isRemoteInvalidated()).isFalse();
    }

    @Test
    void testInvalidateWithRemote() {
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";
        boolean invalidateRemote = true;

        InvalidateResult expectedResult = new InvalidateResult(area, cacheName, key);
        expectedResult.setSuccess(true);
        expectedResult.setLocalInvalidated(true);
        expectedResult.setRemoteInvalidated(true);

        InvalidateRequest expectedRequest = new InvalidateRequest(area, cacheName, key);
        expectedRequest.setInvalidateRemote(invalidateRemote);
        when(localCacheService.invalidate(expectedRequest)).thenReturn(expectedResult);
        InvalidateResult result = controller.invalidate(area, cacheName, key, invalidateRemote);
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.isRemoteInvalidated()).isTrue();
    }

    @Test
    void testInvalidateWithoutKey() {
        String area = "test-area";
        String cacheName = "test-cache";
        boolean invalidateRemote = false;

        InvalidateResult expectedResult = new InvalidateResult(area, cacheName, null);
        expectedResult.setSuccess(false);
        expectedResult.setErrorMessage("Clearing entire cache is not supported");

        InvalidateRequest expectedRequest = new InvalidateRequest(area, cacheName, null);
        expectedRequest.setInvalidateRemote(invalidateRemote);
        when(localCacheService.invalidate(expectedRequest)).thenReturn(expectedResult);
        InvalidateResult result = controller.invalidate(area, cacheName, null, invalidateRemote);
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void testInvalidatePost() {
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", "test-key");
        request.setInvalidateRemote(false);

        InvalidateResult expectedResult = new InvalidateResult("test-area", "test-cache", "test-key");
        expectedResult.setSuccess(true);
        expectedResult.setLocalInvalidated(true);
        when(localCacheService.invalidate(request)).thenReturn(expectedResult);
        InvalidateResult result = controller.invalidatePost(request);
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.isSuccess()).isTrue();
    }
}
