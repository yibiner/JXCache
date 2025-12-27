package dev.yibin.jxcache.registry.spi;

import dev.yibin.jxcache.registry.spi.RegistryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * RegistryClientFactory 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RegistryClientFactoryTest {

    @Mock
    private RegistryClient registryClient1;

    @Mock
    private RegistryClient registryClient2;

    private RegistryClientFactory factory;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        factory = new RegistryClientFactory();
        // 使用反射设置私有字段
        org.springframework.test.util.ReflectionTestUtils.setField(
                factory, "registryClients", Arrays.asList(registryClient1, registryClient2));
    }

    @Test
    void testGetRegistryClientWithValidService() {
        String serviceName = "test-service";
        when(registryClient1.supports(serviceName)).thenReturn(false);
        when(registryClient2.supports(serviceName)).thenReturn(true);
        // registryClient1 不支持，所以不需要设置 getPriority()
        when(registryClient2.getPriority()).thenReturn(200);
        RegistryClient result = factory.getRegistryClient(serviceName);
        assertThat(result).isEqualTo(registryClient2);
    }

    @Test
    void testGetRegistryClientWithPriority() {
        String serviceName = "test-service";
        when(registryClient1.supports(serviceName)).thenReturn(true);
        when(registryClient2.supports(serviceName)).thenReturn(true);
        when(registryClient1.getPriority()).thenReturn(100);  // 更高优先级
        when(registryClient2.getPriority()).thenReturn(200);
        RegistryClient result = factory.getRegistryClient(serviceName);
        assertThat(result).isEqualTo(registryClient1);
    }

    @Test
    void testGetRegistryClientWithNullServiceName() {
        assertThatThrownBy(() -> factory.getRegistryClient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Service name cannot be null or empty");
    }

    @Test
    void testGetRegistryClientWithEmptyServiceName() {
        assertThatThrownBy(() -> factory.getRegistryClient(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Service name cannot be null or empty");
    }

    @Test
    void testGetRegistryClientWithNoMatchingClient() {
        String serviceName = "unknown-service";
        when(registryClient1.supports(serviceName)).thenReturn(false);
        when(registryClient2.supports(serviceName)).thenReturn(false);
        RegistryClient result = factory.getRegistryClient(serviceName);
        assertThat(result).isNull();
    }

    @Test
    void testGetRegistryClientWithNullClients() {
        org.springframework.test.util.ReflectionTestUtils.setField(factory, "registryClients", null);
        RegistryClient result = factory.getRegistryClient("test-service");
        assertThat(result).isNull();
    }

    @Test
    void testGetRegistryClientWithEmptyClients() {
        org.springframework.test.util.ReflectionTestUtils.setField(factory, "registryClients", Arrays.asList());
        RegistryClient result = factory.getRegistryClient("test-service");
        assertThat(result).isNull();
    }

    @Test
    void testGetAllRegistryClients() {
        List<RegistryClient> result = factory.getAllRegistryClients();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(registryClient1, registryClient2);
    }

    @Test
    void testGetAllRegistryClientsWithNull() {
        org.springframework.test.util.ReflectionTestUtils.setField(factory, "registryClients", null);
        List<RegistryClient> result = factory.getAllRegistryClients();
        assertThat(result).isEmpty();
    }

    @Test
    void testGetRegistryClients() {
        String serviceName = "test-service";
        when(registryClient1.supports(serviceName)).thenReturn(true);
        when(registryClient2.supports(serviceName)).thenReturn(true);
        when(registryClient1.getPriority()).thenReturn(100);
        when(registryClient2.getPriority()).thenReturn(200);
        List<RegistryClient> result = factory.getRegistryClients(serviceName);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(registryClient1);  // 按优先级排序
        assertThat(result.get(1)).isEqualTo(registryClient2);
    }
}
