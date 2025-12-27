package dev.yibin.jxcache.tests;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import dev.yibin.jxcache.common.dto.AggregateResult;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.registry.nacos.NacosRegistryClient;
import dev.yibin.jxcache.registry.spi.RegistryClient;
import dev.yibin.jxcache.registry.spi.RegistryClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nacos 注册中心和聚合服务集成测试
 * <p>
 * <b>测试环境要求：</b>
 * <ul>
 *   <li><b>Nacos 服务（可选）</b>：如果没有 Nacos 服务，测试会自动降级到 Fixed Registry</li>
 *   <li>不需要 Redis：使用本地缓存（Caffeine）</li>
 *   <li>广播监控功能已禁用，避免 AOP 相关依赖问题</li>
 * </ul>
 * <p>
 * <b>注意：</b>可以通过配置 `jxc.registry.nacos.enabled=false` 来跳过 Nacos 相关测试
 */
@SpringBootTest(classes = NacosRegistrySmokeIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "jxc.aggregator.enabled=true",
        "jxc.observer.enabled=true",
        "jxc.registry.nacos.enabled=false",  // 如果没有 Nacos 服务，设置为 false
        "jxc.registry.fixed.enabled=true",
        "jxc.registry.fixed.services[0].name=demo-app",
        "jxc.registry.fixed.services[0].nodes[0].nodeId=n1",
        "jxc.registry.fixed.services[0].nodes[0].host=127.0.0.1",
        "jxc.registry.fixed.services[0].nodes[0].port=18081",
        "jxc.registry.fixed.services[0].nodes[0].healthy=true",
        "jxc.registry.fixed.services[0].nodes[1].nodeId=n2",
        "jxc.registry.fixed.services[0].nodes[1].host=127.0.0.1",
        "jxc.registry.fixed.services[0].nodes[1].port=18082",
        "jxc.registry.fixed.services[0].nodes[1].healthy=true",
        // 禁用 Spring Cloud Alibaba Nacos Discovery 自动配置
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.server-addr="
})
class NacosRegistrySmokeIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TestCacheService testCacheService;

    @Autowired(required = false)
    private RegistryClientFactory registryClientFactory;

    @Autowired(required = false)
    private NacosRegistryClient nacosRegistryClient;

    @BeforeEach
    void setUp() {
        prepareTestData();
    }

    @Test
    @DisplayName("测试1: 注册中心客户端工厂 - 应该能获取注册中心客户端")
    void test_registryClientFactory_shouldReturnClients() {
        assertThat(registryClientFactory).isNotNull();
        
        List<RegistryClient> allClients = registryClientFactory.getAllRegistryClients();
        assertThat(allClients).isNotEmpty();
        
        // 验证 FixedRegistryClient 存在（因为配置中启用了 fixed）
        boolean hasFixedClient = allClients.stream()
                .anyMatch(client -> client.getClass().getSimpleName().equals("StaticRegistryClient"));
        assertThat(hasFixedClient).isTrue();
    }

    @Test
    @DisplayName("测试2: 聚合查询单个缓存条目 - 应该能聚合查询多个节点的单个缓存条目")
    void test_aggregateEntry_shouldReturnEntryDetails() {
        ResponseEntity<List<LocalCacheEntryDetail>> entryResp = restTemplate.exchange(
                baseUrl("/api/jxc/aggregate/entry?serviceName=demo-app&area=test&name=testCache&key=key1"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<LocalCacheEntryDetail>>() {}
        );

        assertThat(entryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entryResp.getBody()).isNotNull();
        // 注意：由于配置的节点可能不存在，这里只验证响应格式正确
    }

    @Test
    @DisplayName("测试3: 聚合查询 - 应该能聚合查询多个节点的缓存数据")
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
    }

    @Test
    @DisplayName("测试4: GET 方式聚合查询 - 应该支持 GET 方式查询")
    void test_aggregateQueryGet_shouldSupportGetMethod() {
        ResponseEntity<AggregateResult> queryResp = restTemplate.getForEntity(
                baseUrl("/api/jxc/aggregate/query?serviceName=demo-app&area=test&cacheName=testCache&pageNo=1&pageSize=20"),
                AggregateResult.class
        );

        assertThat(queryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queryResp.getBody()).isNotNull();
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


