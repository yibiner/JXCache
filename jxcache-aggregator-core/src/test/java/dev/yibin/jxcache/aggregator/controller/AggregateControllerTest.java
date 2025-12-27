package dev.yibin.jxcache.aggregator.controller;

import dev.yibin.jxcache.aggregator.service.AggregateQueryService;
import dev.yibin.jxcache.common.dto.AggregateResult;
import dev.yibin.jxcache.common.dto.CacheConsistencyResult;
import dev.yibin.jxcache.common.dto.InvalidateRequest;
import dev.yibin.jxcache.common.dto.InvalidateResult;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.registry.spi.RegistryClientFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * AggregateController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AggregateControllerTest {

    @Mock
    private AggregateQueryService aggregateQueryService;

    @Mock
    private RegistryClientFactory registryClientFactory;

    @InjectMocks
    private AggregateController controller;

    @Test
    void testQueryGet() {
        String serviceName = "test-service";
        List<String> targets = Arrays.asList("n1", "n2");
        String area = "test-area";
        String cacheName = "test-cache";
        String keyPrefix = "test-prefix";
        int pageNo = 1;
        int pageSize = 20;
        int shard = 0;
        int totalShards = 1;

        AggregateResult expectedResult = new AggregateResult();
        when(aggregateQueryService.aggregateQuery(eq(serviceName), any(QueryRequest.class), eq(targets)))
                .thenReturn(expectedResult);
        AggregateResult result = controller.queryGet(serviceName, targets, area, cacheName,
                keyPrefix, pageNo, pageSize, shard, totalShards);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testQueryPost() {
        String serviceName = "test-service";
        List<String> targets = Arrays.asList("n1", "n2");
        QueryRequest request = new QueryRequest();
        request.setArea("test-area");
        request.setCacheName("test-cache");

        AggregateResult expectedResult = new AggregateResult();
        when(aggregateQueryService.aggregateQuery(serviceName, request, targets))
                .thenReturn(expectedResult);
        AggregateResult result = controller.queryPost(serviceName, targets, request);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testQueryGetWithNullTargets() {
        String serviceName = "test-service";
        String area = "test-area";
        String cacheName = "test-cache";

        AggregateResult expectedResult = new AggregateResult();
        when(aggregateQueryService.aggregateQuery(eq(serviceName), any(QueryRequest.class), eq(null)))
                .thenReturn(expectedResult);
        AggregateResult result = controller.queryGet(serviceName, null, area, cacheName,
                null, 1, 20, 0, 1);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testGetEntry() {
        String serviceName = "test-service";
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";
        List<String> targets = Arrays.asList("n1", "n2");

        LocalCacheEntryDetail entry1 = new LocalCacheEntryDetail("n1", area, cacheName);
        entry1.setKey(key);
        LocalCacheEntryDetail entry2 = new LocalCacheEntryDetail("n2", area, cacheName);
        entry2.setKey(key);

        List<LocalCacheEntryDetail> expectedResult = Arrays.asList(entry1, entry2);
        when(aggregateQueryService.aggregateEntry(serviceName, area, cacheName, key, targets))
                .thenReturn(expectedResult);
        List<LocalCacheEntryDetail> result = controller.getEntry(serviceName, area, cacheName, key, targets);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testGetEntryWithNullTargets() {
        String serviceName = "test-service";
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";

        LocalCacheEntryDetail entry = new LocalCacheEntryDetail("n1", area, cacheName);
        entry.setKey(key);
        List<LocalCacheEntryDetail> expectedResult = Arrays.asList(entry);

        when(aggregateQueryService.aggregateEntry(serviceName, area, cacheName, key, null))
                .thenReturn(expectedResult);
        List<LocalCacheEntryDetail> result = controller.getEntry(serviceName, area, cacheName, key, null);
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testCheckConsistency() {
        String serviceName = "test-service";
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";
        List<String> targets = Arrays.asList("n1", "n2");

        CacheConsistencyResult expectedResult = new CacheConsistencyResult(area, cacheName, key);
        expectedResult.setConsistent(true);
        when(aggregateQueryService.checkConsistency(serviceName, area, cacheName, key, targets))
                .thenReturn(expectedResult);
        CacheConsistencyResult result = controller.checkConsistency(serviceName, area, cacheName, key, targets);
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.isConsistent()).isTrue();
    }

    @Test
    void testCheckConsistencyWithNullTargets() {
        String serviceName = "test-service";
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";

        CacheConsistencyResult expectedResult = new CacheConsistencyResult(area, cacheName, key);
        expectedResult.setConsistent(false);
        when(aggregateQueryService.checkConsistency(serviceName, area, cacheName, key, null))
                .thenReturn(expectedResult);
        CacheConsistencyResult result = controller.checkConsistency(serviceName, area, cacheName, key, null);
        assertThat(result).isEqualTo(expectedResult);
        assertThat(result.isConsistent()).isFalse();
    }

    @Test
    void testAggregateInvalidate() {
        String serviceName = "test-service";
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";
        List<String> targets = Arrays.asList("n1", "n2");
        boolean invalidateRemote = false;

        InvalidateResult result1 = new InvalidateResult(area, cacheName, key);
        result1.setNodeId("n1");
        result1.setSuccess(true);
        result1.setLocalInvalidated(true);

        InvalidateResult result2 = new InvalidateResult(area, cacheName, key);
        result2.setNodeId("n2");
        result2.setSuccess(true);
        result2.setLocalInvalidated(true);

        List<InvalidateResult> expectedResults = Arrays.asList(result1, result2);
        when(aggregateQueryService.aggregateInvalidate(eq(serviceName), any(InvalidateRequest.class), eq(targets)))
                .thenReturn(expectedResults);
        List<InvalidateResult> results = controller.aggregateInvalidate(
                serviceName, area, cacheName, key, targets, invalidateRemote);
        assertThat(results).isEqualTo(expectedResults);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(1).isSuccess()).isTrue();
    }

    @Test
    void testAggregateInvalidateWithNullTargets() {
        String serviceName = "test-service";
        String area = "test-area";
        String cacheName = "test-cache";
        String key = "test-key";
        boolean invalidateRemote = true;

        InvalidateResult result = new InvalidateResult(area, cacheName, key);
        result.setNodeId("n1");
        result.setSuccess(true);
        result.setLocalInvalidated(true);
        result.setRemoteInvalidated(true);

        List<InvalidateResult> expectedResults = Arrays.asList(result);
        when(aggregateQueryService.aggregateInvalidate(eq(serviceName), any(InvalidateRequest.class), eq(null)))
                .thenReturn(expectedResults);
        List<InvalidateResult> results = controller.aggregateInvalidate(
                serviceName, area, cacheName, key, null, invalidateRemote);
        assertThat(results).isEqualTo(expectedResults);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isRemoteInvalidated()).isTrue();
    }

    @Test
    void testAggregateInvalidatePost() {
        String serviceName = "test-service";
        List<String> targets = Arrays.asList("n1");
        InvalidateRequest request = new InvalidateRequest("test-area", "test-cache", "test-key");
        request.setInvalidateRemote(false);

        InvalidateResult result = new InvalidateResult("test-area", "test-cache", "test-key");
        result.setNodeId("n1");
        result.setSuccess(true);
        result.setLocalInvalidated(true);

        List<InvalidateResult> expectedResults = Arrays.asList(result);
        when(aggregateQueryService.aggregateInvalidate(serviceName, request, targets))
                .thenReturn(expectedResults);
        List<InvalidateResult> results = controller.aggregateInvalidatePost(serviceName, targets, request);
        assertThat(results).isEqualTo(expectedResults);
        assertThat(results).hasSize(1);
    }
}
