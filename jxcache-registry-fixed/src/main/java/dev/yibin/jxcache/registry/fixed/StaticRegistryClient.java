package dev.yibin.jxcache.registry.fixed;

import dev.yibin.jxcache.common.dto.ServiceInstance;
import dev.yibin.jxcache.registry.spi.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 静态注册中心客户端实现
 * 基于配置文件提供静态服务实例列表
 * 
 * 优先级较低，通常作为降级方案使用
 */
@Component
@ConditionalOnProperty(prefix = "jxc.registry.fixed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StaticRegistryClient implements RegistryClient {

    private static final Logger logger = LoggerFactory.getLogger(StaticRegistryClient.class);
    private static final int DEFAULT_PRIORITY = 1000; // 较低优先级，作为降级方案

    @Autowired(required = false)
    private StaticRegistryProperties properties;
    
    @PostConstruct
    public void init() {
        int serviceCount = properties != null && properties.getServices() != null 
                ? properties.getServices().size() : 0;
        logger.info("[FixedRegistry] Static registry client enabled and initialized, configured services: {}", serviceCount);
    }

    @Override
    public List<ServiceInstance> listInstances(String serviceName) {
        if (properties == null || properties.getServices() == null || serviceName == null || serviceName.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        return properties.getServices().stream()
                .filter(service -> service != null && serviceName.equals(service.getName()))
                .flatMap(service -> service.getNodes() != null ? service.getNodes().stream() : new java.util.ArrayList<StaticRegistryProperties.NodeConfig>().stream())
                .filter(node -> node != null && node.isHealthy())
                .map(this::convertToServiceInstance)
                .collect(Collectors.toList());
    }

    @Override
    public ServiceInstance current() {
        return null;
    }

    @Override
    public boolean supports(String serviceName) {
        if (properties == null || properties.getServices() == null || serviceName == null || serviceName.isEmpty()) {
            return false;
        }

        return properties.getServices().stream()
                .anyMatch(service -> service != null && serviceName.equals(service.getName()));
    }

    @Override
    public int getPriority() {
        // 如果配置了优先级，使用配置值；否则使用默认值
        if (properties != null) {
            Integer priority = properties.getPriority();
            if (priority != null) {
                return priority;
            }
        }
        return DEFAULT_PRIORITY;
    }

    /**
     * 转换为服务实例
     */
    private ServiceInstance convertToServiceInstance(StaticRegistryProperties.NodeConfig nodeConfig) {
        if (nodeConfig == null) {
            return null;
        }

        ServiceInstance instance = new ServiceInstance();
        instance.setNodeId(nodeConfig.getNodeId());
        instance.setHost(nodeConfig.getHost());
        instance.setPort(nodeConfig.getPort());
        instance.setHealthy(nodeConfig.isHealthy());
        instance.setMetadata(nodeConfig.getMetadata());
        return instance;
    }
}
