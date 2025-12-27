package dev.yibin.jxcache.registry.fixed;

import dev.yibin.jxcache.common.dto.ServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * StaticRegistryClient 单元测试
 */
@ExtendWith(MockitoExtension.class)
class StaticRegistryClientTest {

    @Mock
    private StaticRegistryProperties properties;

    @InjectMocks
    private StaticRegistryClient client;

    private StaticRegistryProperties.ServiceConfig serviceConfig;

    @BeforeEach
    void setUp() {
        serviceConfig = new StaticRegistryProperties.ServiceConfig();
        serviceConfig.setName("demo-app");
        serviceConfig.setNodes(Arrays.asList(
                node("n1", "127.0.0.1", 18081, true),
                node("n2", "127.0.0.1", 18082, true)
        ));
    }

    private StaticRegistryProperties.NodeConfig node(String id, String host, int port, boolean healthy) {
        StaticRegistryProperties.NodeConfig n = new StaticRegistryProperties.NodeConfig();
        n.setNodeId(id);
        n.setHost(host);
        n.setPort(port);
        n.setHealthy(healthy);
        return n;
    }

    @Test
    @DisplayName("listInstances：已配置服务返回健康节点")
    void listInstances_returnsHealthyNodes() {
        when(properties.getServices()).thenReturn(Arrays.asList(serviceConfig));

        List<ServiceInstance> instances = client.listInstances("demo-app");

        assertThat(instances).hasSize(2);
        assertThat(instances.get(0).getNodeId()).isEqualTo("n1");
        assertThat(instances.get(0).getHost()).isEqualTo("127.0.0.1");
        assertThat(instances.get(0).getPort()).isEqualTo(18081);
        assertThat(instances.get(1).getNodeId()).isEqualTo("n2");
        assertThat(instances.get(1).getHost()).isEqualTo("127.0.0.1");
        assertThat(instances.get(1).getPort()).isEqualTo(18082);

        verify(properties, atLeastOnce()).getServices();
        verifyNoMoreInteractions(properties);
    }

    @Test
    @DisplayName("listInstances：未知服务返回空列表")
    void listInstances_unknownService_returnsEmpty() {
        when(properties.getServices()).thenReturn(Arrays.asList(serviceConfig));

        List<ServiceInstance> instances = client.listInstances("unknown");
        assertThat(instances).isEmpty();
    }

    @Test
    @DisplayName("listInstances：服务配置为空时返回空列表")
    void listInstances_nullServices_returnsEmpty() {
        when(properties.getServices()).thenReturn(null);

        List<ServiceInstance> instances = client.listInstances("demo-app");
        assertThat(instances).isEmpty();
    }

    @Test
    @DisplayName("listInstances：节点列表为空时返回空列表")
    void listInstances_emptyOrNullNodes_returnsEmpty() {
        StaticRegistryProperties.ServiceConfig emptyNodes = new StaticRegistryProperties.ServiceConfig();
        emptyNodes.setName("demo-app");
        emptyNodes.setNodes(Collections.emptyList());
        when(properties.getServices()).thenReturn(Arrays.asList(emptyNodes));
        assertThat(client.listInstances("demo-app")).isEmpty();

        StaticRegistryProperties.ServiceConfig nullNodes = new StaticRegistryProperties.ServiceConfig();
        nullNodes.setName("demo-app");
        nullNodes.setNodes(null);
        when(properties.getServices()).thenReturn(Arrays.asList(nullNodes));
        assertThat(client.listInstances("demo-app")).isEmpty();
    }

    @Test
    @DisplayName("listInstances：过滤不健康节点")
    void listInstances_filtersUnhealthy() {
        StaticRegistryProperties.ServiceConfig cfg = new StaticRegistryProperties.ServiceConfig();
        cfg.setName("demo-app");
        cfg.setNodes(Arrays.asList(
                node("n1", "127.0.0.1", 18081, true),
                node("nX", "127.0.0.1", 19000, false)
        ));
        when(properties.getServices()).thenReturn(Arrays.asList(cfg));

        List<ServiceInstance> instances = client.listInstances("demo-app");
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getNodeId()).isEqualTo("n1");
    }

    @Test
    @DisplayName("supports：已配置服务返回 true，否则返回 false")
    void supports_cases() {
        when(properties.getServices()).thenReturn(Arrays.asList(serviceConfig));
        assertThat(client.supports("demo-app")).isTrue();
        assertThat(client.supports("unknown-service")).isFalse();

        when(properties.getServices()).thenReturn(null);
        assertThat(client.supports("demo-app")).isFalse();

        when(properties.getServices()).thenReturn(Arrays.asList(serviceConfig));
        assertThat(client.supports(null)).isFalse();
        assertThat(client.supports("")).isFalse();
        assertThat(client.supports("   ")).isFalse();
    }

    @Test
    @DisplayName("current：静态注册中心返回 null")
    void current_returnsNull() {
        assertThat(client.current()).isNull();
    }
}
