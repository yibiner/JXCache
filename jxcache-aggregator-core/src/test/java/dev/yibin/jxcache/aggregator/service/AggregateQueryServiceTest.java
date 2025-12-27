package dev.yibin.jxcache.aggregator.service;

import dev.yibin.jxcache.common.dto.AggregateResult;
import dev.yibin.jxcache.common.dto.CacheConsistencyResult;
import dev.yibin.jxcache.common.dto.InvalidateRequest;
import dev.yibin.jxcache.common.dto.InvalidateResult;
import dev.yibin.jxcache.common.dto.QueryRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AggregateQueryService 单元测试
 * 
 * 注意：由于 AggregateQueryService 使用 @PostConstruct 初始化 ExecutorService，
 * 且依赖复杂的异步操作，这里主要测试参数验证和边界情况
 */
@ExtendWith(MockitoExtension.class)
class AggregateQueryServiceTest {

    @Test
    void testAggregateQueryWithNullServiceName() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");

        QueryRequest request = new QueryRequest();
        request.setArea("area1");
        request.setCacheName("cache1");
        AggregateResult result = service.aggregateQuery(null, request, null);
        assertThat(result).isNotNull();
        assertThat(result.getResults()).isEmpty();
        assertThat(result.getFailedNodes()).isEmpty();
        assertThat(result.isPartial()).isFalse();
    }

    @Test
    void testAggregateQueryWithNullRequest() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");
        AggregateResult result = service.aggregateQuery("test-service", null, null);
        assertThat(result).isNotNull();
        assertThat(result.getResults()).isEmpty();
        assertThat(result.getFailedNodes()).isEmpty();
        assertThat(result.isPartial()).isFalse();
    }

    @Test
    void testAggregateEntryWithNullServiceName() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");
        java.util.List<dev.yibin.jxcache.common.dto.LocalCacheEntryDetail> result = 
                service.aggregateEntry(null, "area1", "cache1", "key1", null);
        assertThat(result).isEmpty();
    }

    @Test
    void testAggregateEntryWithInvalidParameters() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");
        java.util.List<dev.yibin.jxcache.common.dto.LocalCacheEntryDetail> result1 = 
                service.aggregateEntry("test-service", null, "cache1", "key1", null);
        java.util.List<dev.yibin.jxcache.common.dto.LocalCacheEntryDetail> result2 = 
                service.aggregateEntry("test-service", "area1", null, "key1", null);
        java.util.List<dev.yibin.jxcache.common.dto.LocalCacheEntryDetail> result3 = 
                service.aggregateEntry("test-service", "area1", "cache1", null, null);
        java.util.List<dev.yibin.jxcache.common.dto.LocalCacheEntryDetail> result4 = 
                service.aggregateEntry("test-service", "", "cache1", "key1", null);
        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();
        assertThat(result3).isEmpty();
        assertThat(result4).isEmpty();
    }

    @Test
    void testCheckConsistencyWithNullServiceName() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");
        CacheConsistencyResult result = service.checkConsistency(null, "area1", "cache1", "key1", null);
        assertThat(result).isNotNull();
        assertThat(result.isConsistent()).isFalse();
        assertThat(result.getConsistencyDetail()).contains("Service name is null or empty");
        assertThat(result.getEntries()).isEmpty();
        assertThat(result.getFailedNodes()).isEmpty();
    }

    @Test
    void testCheckConsistencyWithInvalidParameters() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");
        CacheConsistencyResult result1 = service.checkConsistency("test-service", null, "cache1", "key1", null);
        CacheConsistencyResult result2 = service.checkConsistency("test-service", "area1", null, "key1", null);
        CacheConsistencyResult result3 = service.checkConsistency("test-service", "area1", "cache1", null, null);
        CacheConsistencyResult result4 = service.checkConsistency("test-service", "", "cache1", "key1", null);
        assertThat(result1.isConsistent()).isFalse();
        assertThat(result1.getConsistencyDetail()).contains("Invalid parameters");
        assertThat(result2.isConsistent()).isFalse();
        assertThat(result3.isConsistent()).isFalse();
        assertThat(result4.isConsistent()).isFalse();
    }

    @Test
    void testAggregateInvalidateWithNullServiceName() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");

        InvalidateRequest request = new InvalidateRequest("area1", "cache1", "key1");
        java.util.List<InvalidateResult> result = service.aggregateInvalidate(null, request, null);
        assertThat(result).isEmpty();
    }

    @Test
    void testAggregateInvalidateWithNullRequest() {
        AggregateQueryService service = new AggregateQueryService();
        ReflectionTestUtils.setField(service, "registryClientFactory", null);
        ReflectionTestUtils.setField(service, "restTemplate", null);
        ReflectionTestUtils.setField(service, "perNodeTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "totalTimeoutMs", 4000);
        ReflectionTestUtils.setField(service, "maxConcurrency", 16);
        ReflectionTestUtils.setField(service, "observerScanPath", "/api/jxc/observer/query");
        ReflectionTestUtils.setField(service, "observerEntryPath", "/api/jxc/observer/entry");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.setField(service, "observerInvalidatePath", "/api/jxc/observer/invalidate");
        ReflectionTestUtils.invokeMethod(service, "init");
        java.util.List<InvalidateResult> result = service.aggregateInvalidate("test-service", null, null);
        assertThat(result).isEmpty();
    }
}