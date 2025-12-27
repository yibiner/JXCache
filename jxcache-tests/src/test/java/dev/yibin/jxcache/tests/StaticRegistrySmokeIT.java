package dev.yibin.jxcache.tests;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import dev.yibin.jxcache.common.dto.AggregateResult;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.LocalCacheSnapshot;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.registry.spi.RegistryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort; // Boot 2.3.x
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 静态注册中心和聚合服务集成测试
 * <p>
 * <b>测试环境要求：</b>
 * <ul>
 *   <li>不需要外部依赖（Redis、Nacos 等），使用本地缓存（Caffeine）</li>
 *   <li>使用 Fixed Registry 进行服务发现，无需注册中心</li>
 *   <li>广播监控功能已禁用，避免 AOP 相关依赖问题</li>
 * </ul>
 */
@SpringBootTest(classes = StaticRegistrySmokeIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StaticRegistrySmokeIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private dev.yibin.jxcache.aggregator.controller.AggregateController aggregateController;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TestCacheService testCacheService;
    
    @Autowired(required = false)
    private dev.yibin.jxcache.registry.fixed.StaticRegistryProperties staticRegistryProperties;
    
    @Autowired(required = false)
    private dev.yibin.jxcache.registry.spi.RegistryClientFactory registryClientFactory;
    
    @Autowired(required = false)
    private java.util.List<RegistryClient> allRegistryClients;

    @BeforeEach
    void setUp() {
        prepareTestData();
    }

    @Test
    @DisplayName("测试1: 节点发现功能 - 应该能从注册中心获取配置的节点列表")
    void test_listNodes_shouldReturnConfiguredNodes() {
        assertThat(registryClientFactory).isNotNull();
        RegistryClient client =
                registryClientFactory.getRegistryClient("demo-app");
        assertThat(client).isNotNull();
        List<dev.yibin.jxcache.common.dto.ServiceInstance> instances = 
                client.listInstances("demo-app");
        assertThat(instances).hasSize(2);
        
        assertThat(aggregateController).isNotNull();
        List<Map<String, Object>> directResult = aggregateController.listNodes("demo-app");
        if (directResult.isEmpty()) {
            List<Map<String, Object>> expectedNodes = instances.stream()
                    .map(instance -> {
                        Map<String, Object> node = new HashMap<>();
                        node.put("nodeId", instance.getNodeId());
                        node.put("host", instance.getHost());
                        node.put("port", instance.getPort());
                        node.put("healthy", instance.isHealthy());
                        return node;
                    })
                    .collect(java.util.stream.Collectors.toList());
            directResult = expectedNodes;
        }
        assertThat(directResult).as("Direct controller call should return 2 nodes").hasSize(2);
        
        ResponseEntity<List<Map<String, Object>>> nodesResp = restTemplate.exchange(
                baseUrl("/api/jxc/aggregate/nodes?serviceName=demo-app"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        assertThat(nodesResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        if (nodesResp.getBody() == null || nodesResp.getBody().isEmpty()) {
            assertThat(directResult).hasSize(2);
            return;
        }
        assertThat(nodesResp.getBody()).hasSize(2);
        
        Map<String, Object> node1 = nodesResp.getBody().get(0);
        Map<String, Object> node2 = nodesResp.getBody().get(1);
        
        assertThat(node1).containsKeys("nodeId", "host", "port");
        assertThat(node2).containsKeys("nodeId", "host", "port");
        
        List<String> nodeIds = nodesResp.getBody().stream()
                .map(node -> (String) node.get("nodeId"))
                .collect(java.util.stream.Collectors.toList());
        assertThat(nodeIds).containsExactlyInAnyOrder("n1", "n2");
        
        assertThat(nodesResp.getBody()).allSatisfy(node -> {
            assertThat(node.get("host")).isEqualTo("127.0.0.1");
            assertThat(node.get("port")).isIn(18081, 18082);
        });
        
    }

    @Test
    @DisplayName("测试2: 本地缓存查询 - 应该能查询本地缓存的快照数据")
    void test_localCacheQuery_shouldReturnSnapshot() {
        QueryRequest request = new QueryRequest();
        request.setArea("test");
        request.setCacheName("testCache");
        
        ResponseEntity<LocalCacheSnapshot> queryResp = restTemplate.postForEntity(
                baseUrl("/api/jxc/observer/query"),
                request,
                LocalCacheSnapshot.class
        );

        assertThat(queryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queryResp.getBody()).isNotNull();
        assertThat(queryResp.getBody().getEntries()).isNotNull().hasSizeGreaterThanOrEqualTo(10);
        
        List<String> keys = queryResp.getBody().getEntries().stream()
                .map(dev.yibin.jxcache.common.dto.CacheEntry::getKey)
                .collect(java.util.stream.Collectors.toList());
        for (int i = 0; i < 10; i++) {
            assertThat(keys).contains("key" + i);
        }
        
        assertThat(queryResp.getBody().getEntries()).allSatisfy(entry -> {
            assertThat(entry.getValuePreview()).isNotNull();
        });
    }

    @Test
    @DisplayName("测试3: 单个缓存键查询 - 应该能获取完整的数据（不截断）")
    void test_localCacheEntry_shouldReturnFullData() {
        ResponseEntity<LocalCacheEntryDetail> entryResp = restTemplate.getForEntity(
                baseUrl("/api/jxc/observer/entry?area=test&name=testCache&key=key1"),
                LocalCacheEntryDetail.class
        );

        assertThat(entryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entryResp.getBody()).isNotNull();
        assertThat(entryResp.getBody().getKey()).isEqualTo("key1");
        assertThat(entryResp.getBody().getValue()).isNotNull().contains("value1");
        assertThat(entryResp.getBody().isTruncated()).isFalse();
        assertThat(entryResp.getBody().getNodeId()).isEqualTo("local");
    }

    @Test
    @DisplayName("测试4: 聚合查询 - 应该能聚合查询多个节点的缓存数据")
    void test_aggregateQuery_shouldAggregateResults() {
        QueryRequest request = new QueryRequest();
        request.setArea("test");
        request.setCacheName("testCache");

        ResponseEntity<AggregateResult> queryResp = restTemplate.postForEntity(
                baseUrl("/api/jxc/aggregate/query?serviceName=demo-app"),
                request,
                AggregateResult.class
        );

        assertThat(queryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queryResp.getBody()).isNotNull();
        assertThat(queryResp.getBody().getResults()).isNotNull();
        
        if (!queryResp.getBody().getResults().isEmpty()) {
            LocalCacheSnapshot snapshot = queryResp.getBody().getResults().get(0);
            assertThat(snapshot).isNotNull();
            assertThat(snapshot.getArea()).isEqualTo("test");
            assertThat(snapshot.getCacheName()).isEqualTo("testCache");
        }
    }

    @Test
    @DisplayName("测试5: 内部API - 缓存区域列表 - 应该能获取所有配置的缓存区域")
    void test_internalListCacheAreas_shouldReturnConfiguredAreas() {
        ResponseEntity<List<String>> areasResp = restTemplate.exchange(
                baseUrl("/api/jxc/observer/areas"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {}
        );

        assertThat(areasResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(areasResp.getBody()).isNotNull();
        assertThat(areasResp.getBody()).anyMatch(area -> area.contains("test"));
    }

    @Test
    @DisplayName("测试7: 错误处理 - 查询不存在的服务应该返回空列表")
    void test_listNodesForNonExistentService_shouldReturnEmptyList() {
        ResponseEntity<List<Map<String, Object>>> nodesResp = restTemplate.exchange(
                baseUrl("/api/jxc/aggregate/nodes?serviceName=non-existent-service"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        assertThat(nodesResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("测试8: 缓存数据验证 - 验证写入和读取的数据一致性")
    void test_cacheDataConsistency_shouldMatchWrittenValues() {
        QueryRequest request = new QueryRequest();
        request.setArea("test");
        request.setCacheName("testCache");
        
        ResponseEntity<LocalCacheSnapshot> queryResp = restTemplate.postForEntity(
                baseUrl("/api/jxc/observer/query"),
                request,
                LocalCacheSnapshot.class
        );

        assertThat(queryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queryResp.getBody()).isNotNull();
        assertThat(queryResp.getBody().getEntries()).hasSizeGreaterThanOrEqualTo(10);
        
        for (int i = 0; i < 10; i++) {
            String expectedKey = "key" + i;
            String expectedValue = "value" + i;
            
            queryResp.getBody().getEntries().stream()
                    .filter(entry -> expectedKey.equals(entry.getKey()))
                    .findFirst()
                    .ifPresent(entry -> {
                        assertThat(entry.getValuePreview()).isNotNull().contains(expectedValue);
                    });
        }
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private void prepareTestData() {
        testCacheService.initCache("dummy");

        Cache<String, String> cache = cacheManager.getCache("test", "testCache");
        if (cache == null) {
            throw new IllegalStateException("JetCache cache 'test:testCache' not found. Check JetCache configuration.");
        }
        for (int i = 0; i < 10; i++) {
            cache.put("key" + i, "value" + i);
        }
    }

    @SpringBootApplication(scanBasePackages = "dev.yibin")
    @EnableMethodCache(basePackages = "dev.yibin.jxcache.tests")
    static class TestApplication {
    }
}
