package dev.yibin.jxcache.registry.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 注册中心客户端工厂
 * 负责根据服务名称选择合适的注册中心客户端，支持优先级和降级策略
 * 
 * 当多个 RegistryClient 都支持同一服务时，按优先级选择（数值越小优先级越高）
 * @author Yibin
 * @since 1.0.0
 */
@Component
public class RegistryClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(RegistryClientFactory.class);

    @Autowired(required = false)
    private List<RegistryClient> registryClients;

    /**
     * 获取支持指定服务的注册中心客户端
     * 按优先级选择，如果多个客户端支持同一服务，返回优先级最高的
     * 
     * @param serviceName 服务名称
     * @return 注册中心客户端，如果找不到则返回 null
     */
    public RegistryClient getRegistryClient(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }

        if (registryClients == null || registryClients.isEmpty()) {
            logger.warn("[RegistryFactory] No registry clients available");
            return null;
        }

        // 找到所有支持该服务的客户端，按优先级排序
        List<RegistryClient> supportingClients = registryClients.stream()
                .filter(client -> client != null && client.supports(serviceName))
                .sorted(Comparator.comparingInt(RegistryClient::getPriority))
                .collect(Collectors.toList());

        if (supportingClients.isEmpty()) {
            logger.debug("[RegistryFactory] No registry client found for service: {}", serviceName);
            return null;
        }

        RegistryClient selected = supportingClients.get(0);
        logger.debug("[RegistryFactory] Selected registry client '{}' (priority: {}) for service: {}", 
                selected.getName(), selected.getPriority(), serviceName);
        
        return selected;
    }

    /**
     * 获取支持指定服务的所有注册中心客户端（按优先级排序）
     * 
     * @param serviceName 服务名称
     * @return 支持该服务的客户端列表，按优先级从高到低排序
     */
    public List<RegistryClient> getRegistryClients(String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }

        if (registryClients == null || registryClients.isEmpty()) {
            return Collections.emptyList();
        }

        return registryClients.stream()
                .filter(client -> client != null && client.supports(serviceName))
                .sorted(Comparator.comparingInt(RegistryClient::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有注册中心客户端
     * 
     * @return 所有注册中心客户端列表
     */
    public List<RegistryClient> getAllRegistryClients() {
        return registryClients != null ? new ArrayList<>(registryClients) : Collections.emptyList();
    }
}
