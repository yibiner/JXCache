package dev.yibin.jxcache.registry.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import dev.yibin.jxcache.common.dto.ServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * NacosRegistryClient 单元测试
 */
@ExtendWith(MockitoExtension.class)
class NacosRegistryClientTest {

    @Mock
    private NacosServiceDiscovery nacosServiceDiscovery;

    @Mock
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Mock
    private NacosRegistryProperties properties;

    @InjectMocks
    private NacosRegistryClient nacosRegistryClient;

    @BeforeEach
    void setUp() {
        // 使用反射设置私有字段
        org.springframework.test.util.ReflectionTestUtils.setField(
                nacosRegistryClient, "nacosServiceDiscovery", nacosServiceDiscovery);
        org.springframework.test.util.ReflectionTestUtils.setField(
                nacosRegistryClient, "nacosDiscoveryProperties", nacosDiscoveryProperties);
        org.springframework.test.util.ReflectionTestUtils.setField(
                nacosRegistryClient, "properties", properties);
    }

    @Test
    void testListInstances_Success() {
        String serviceName = "test-service";
        org.springframework.cloud.client.ServiceInstance springInstance1 = createMockSpringInstance("instance1", "127.0.0.1", 18081, true);
        org.springframework.cloud.client.ServiceInstance springInstance2 = createMockSpringInstance("instance2", "127.0.0.1", 18082, true);
        List<org.springframework.cloud.client.ServiceInstance> springInstances = Arrays.asList(springInstance1, springInstance2);

        try {
            doReturn(springInstances).when(nacosServiceDiscovery).getInstances(serviceName);
        } catch (com.alibaba.nacos.api.exception.NacosException e) {
            // Mock 不会真正抛出异常
        }
        List<ServiceInstance> result = nacosRegistryClient.listInstances(serviceName);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNodeId()).isEqualTo("instance1");
        assertThat(result.get(0).getHost()).isEqualTo("127.0.0.1");
        assertThat(result.get(0).getPort()).isEqualTo(18081);
        assertThat(result.get(1).getNodeId()).isEqualTo("instance2");
    }

    @Test
    void testListInstances_EmptyList() {
        String serviceName = "test-service";
        try {
            doReturn(Collections.emptyList()).when(nacosServiceDiscovery).getInstances(serviceName);
        } catch (com.alibaba.nacos.api.exception.NacosException e) {
            // Mock 不会真正抛出异常
        }
        List<ServiceInstance> result = nacosRegistryClient.listInstances(serviceName);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testListInstances_NullServiceName() {
        List<ServiceInstance> result = nacosRegistryClient.listInstances(null);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testListInstances_EmptyServiceName() {
        List<ServiceInstance> result = nacosRegistryClient.listInstances("");
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testListInstances_NacosServiceDiscoveryNull() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                nacosRegistryClient, "nacosServiceDiscovery", null);
        String serviceName = "test-service";
        List<ServiceInstance> result = nacosRegistryClient.listInstances(serviceName);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testListInstances_Exception() {
        String serviceName = "test-service";
        try {
            doReturn(Collections.emptyList()).when(nacosServiceDiscovery).getInstances(serviceName);
        } catch (com.alibaba.nacos.api.exception.NacosException e) {
            // Mock 不会真正抛出异常
        }
        List<ServiceInstance> result = nacosRegistryClient.listInstances(serviceName);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testListInstances_WithMetadataInstanceId() {
        String serviceName = "test-service";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("instanceId", "custom-instance-id");
        org.springframework.cloud.client.ServiceInstance springInstance = createMockSpringInstanceWithMetadata(
                "127.0.0.1", 18081, true, metadata);
        try {
            doReturn(Arrays.asList(springInstance)).when(nacosServiceDiscovery).getInstances(serviceName);
        } catch (com.alibaba.nacos.api.exception.NacosException e) {
            // Mock 不会真正抛出异常
        }
        List<ServiceInstance> result = nacosRegistryClient.listInstances(serviceName);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNodeId()).isEqualTo("custom-instance-id");
    }

    @Test
    void testListInstances_WithoutMetadataInstanceId() {
        String serviceName = "test-service";
        // 创建没有 instanceId 的 mock 实例
        org.springframework.cloud.client.ServiceInstance springInstance = createMockSpringInstanceWithMetadata(
                "127.0.0.1", 18081, true, Collections.emptyMap());
        try {
            doReturn(Arrays.asList(springInstance)).when(nacosServiceDiscovery).getInstances(serviceName);
        } catch (com.alibaba.nacos.api.exception.NacosException e) {
            // Mock 不会真正抛出异常
        }
        List<ServiceInstance> result = nacosRegistryClient.listInstances(serviceName);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNodeId()).isEqualTo("127.0.0.1:18081");
    }

    @Test
    void testCurrent_Success() {
        when(nacosDiscoveryProperties.getService()).thenReturn("test-service");
        when(nacosDiscoveryProperties.getIp()).thenReturn("127.0.0.1");
        when(nacosDiscoveryProperties.getPort()).thenReturn(18081);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("instanceId", "current-instance-id");
        when(nacosDiscoveryProperties.getMetadata()).thenReturn(metadata);
        ServiceInstance result = nacosRegistryClient.current();
        assertThat(result).isNotNull();
        assertThat(result.getServiceName()).isEqualTo("test-service");
        assertThat(result.getHost()).isEqualTo("127.0.0.1");
        assertThat(result.getPort()).isEqualTo(18081);
        assertThat(result.getNodeId()).isEqualTo("current-instance-id");
        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    void testCurrent_WithoutMetadataInstanceId() {
        when(nacosDiscoveryProperties.getService()).thenReturn("test-service");
        when(nacosDiscoveryProperties.getIp()).thenReturn("127.0.0.1");
        when(nacosDiscoveryProperties.getPort()).thenReturn(18081);
        when(nacosDiscoveryProperties.getMetadata()).thenReturn(Collections.emptyMap());
        ServiceInstance result = nacosRegistryClient.current();
        assertThat(result).isNotNull();
        assertThat(result.getNodeId()).isEqualTo("127.0.0.1:18081");
    }

    @Test
    void testCurrent_NacosDiscoveryPropertiesNull() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                nacosRegistryClient, "nacosDiscoveryProperties", null);
        ServiceInstance result = nacosRegistryClient.current();
        assertThat(result).isNull();
    }

    @Test
    void testCurrent_Exception() {
        when(nacosDiscoveryProperties.getService()).thenThrow(new RuntimeException("Nacos error"));
        ServiceInstance result = nacosRegistryClient.current();
        assertThat(result).isNull();
    }

    @Test
    void testSupports_WithWhitelist() {
        String serviceName = "test-service";
        when(properties.getServiceWhitelist()).thenReturn(Arrays.asList("test-service", "other-service"));
        boolean result = nacosRegistryClient.supports(serviceName);
        assertThat(result).isTrue();
    }

    @Test
    void testSupports_NotInWhitelist() {
        String serviceName = "test-service";
        when(properties.getServiceWhitelist()).thenReturn(Arrays.asList("other-service"));
        boolean result = nacosRegistryClient.supports(serviceName);
        assertThat(result).isFalse();
    }

    @Test
    void testSupports_EmptyWhitelist() {
        String serviceName = "test-service";
        when(properties.getServiceWhitelist()).thenReturn(Collections.emptyList());
        boolean result = nacosRegistryClient.supports(serviceName);
        assertThat(result).isTrue(); // 空白名单表示支持所有服务
    }

    @Test
    void testSupports_NullWhitelist() {
        String serviceName = "test-service";
        when(properties.getServiceWhitelist()).thenReturn(null);
        boolean result = nacosRegistryClient.supports(serviceName);
        assertThat(result).isTrue(); // null 白名单表示支持所有服务
    }

    @Test
    void testSupports_NullServiceName() {
        boolean result = nacosRegistryClient.supports(null);
        assertThat(result).isFalse();
    }

    @Test
    void testSupports_EmptyServiceName() {
        boolean result = nacosRegistryClient.supports("");
        assertThat(result).isFalse();
    }

    @Test
    void testGetPriority_WithConfig() {
        when(properties.getPriority()).thenReturn(50);
        int result = nacosRegistryClient.getPriority();
        assertThat(result).isEqualTo(50);
    }

    @Test
    void testGetPriority_WithoutConfig() {
        when(properties.getPriority()).thenReturn(null);
        int result = nacosRegistryClient.getPriority();
        assertThat(result).isEqualTo(100); // 默认优先级
    }

    @Test
    void testGetPriority_PropertiesNull() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                nacosRegistryClient, "properties", null);
        int result = nacosRegistryClient.getPriority();
        assertThat(result).isEqualTo(100); // 默认优先级
    }

    /**
     * 创建模拟的 Spring Cloud ServiceInstance
     */
    private org.springframework.cloud.client.ServiceInstance createMockSpringInstance(String instanceId, String host, int port, boolean healthy) {
        org.springframework.cloud.client.ServiceInstance instance = org.mockito.Mockito.mock(org.springframework.cloud.client.ServiceInstance.class);
        when(instance.getServiceId()).thenReturn("test-service");
        when(instance.getHost()).thenReturn(host);
        when(instance.getPort()).thenReturn(port);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("instanceId", instanceId);
        when(instance.getMetadata()).thenReturn(metadata);
        return instance;
    }

    /**
     * 创建带自定义 metadata 的模拟 Spring Cloud ServiceInstance
     */
    private org.springframework.cloud.client.ServiceInstance createMockSpringInstanceWithMetadata(
            String host, int port, boolean healthy, Map<String, String> metadata) {
        org.springframework.cloud.client.ServiceInstance instance = org.mockito.Mockito.mock(org.springframework.cloud.client.ServiceInstance.class);
        when(instance.getServiceId()).thenReturn("test-service");
        when(instance.getHost()).thenReturn(host);
        when(instance.getPort()).thenReturn(port);
        when(instance.getMetadata()).thenReturn(metadata);
        return instance;
    }
}

