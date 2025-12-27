package dev.yibin.jxcache.registry.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import dev.yibin.jxcache.common.dto.ServiceInstance;
import dev.yibin.jxcache.registry.spi.RegistryClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Nacos 注册中心客户端实现
 * 基于 Nacos 服务发现获取服务实例列表
 * <p>
 * 优先级较高（默认100），优先于 FixedRegistryClient 使用
 */
@Component
@ConditionalOnClass(NacosServiceDiscovery.class)
@ConditionalOnProperty(prefix = "jxc.registry.nacos", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NacosRegistryClient implements RegistryClient {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistryClient.class);
    private static final int DEFAULT_PRIORITY = 100; // 较高优先级，优先使用

    @Autowired(required = false)
    private NacosServiceDiscovery nacosServiceDiscovery;

    @Autowired(required = false)
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired(required = false)
    private NacosRegistryProperties properties;
    
    /**
     * 服务实例列表缓存
     * Key: serviceName, Value: List<ServiceInstance>
     */
    private Cache<String, List<ServiceInstance>> instanceCache;
    
    /**
     * 监听器执行器（用于异步更新缓存）
     */
    private ExecutorService listenerExecutor;
    
    /**
     * 监听器是否已初始化
     */
    private final AtomicBoolean listenerInitialized = new AtomicBoolean(false);
    
    /**
     * 初始化缓存
     */
    @PostConstruct
    public void init() {
        logger.info("[NacosRegistry] Nacos registry client enabled and initialized");
        
        if (properties != null && properties.getCache() != null 
                && properties.getCache().isEnabled()) {
            NacosRegistryProperties.Cache cacheConfig = properties.getCache();
            int expireSeconds = cacheConfig.getExpireSeconds() > 0 
                ? cacheConfig.getExpireSeconds() : 10;
            int maxSize = cacheConfig.getMaxSize() > 0 
                ? cacheConfig.getMaxSize() : 1000;
            
            instanceCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build();
            
            logger.debug("[NacosRegistry] Instance cache initialized: expireSeconds={}, maxSize={}", 
                    expireSeconds, maxSize);
            
            // 如果启用监听，初始化监听器
            if (cacheConfig.isEnableListener()) {
                initListener();
            }
        } else {
            logger.debug("[NacosRegistry] Instance cache is disabled");
        }
    }
    
    /**
     * 初始化服务变更监听器
     * 注意：Spring Cloud Alibaba 2.2.5 版本可能不支持直接监听，需要根据实际版本调整
     */
    private void initListener() {
        if (listenerInitialized.compareAndSet(false, true)) {
            try {
                // 创建单线程执行器，确保更新操作的顺序性
                listenerExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "nacos-registry-listener");
                    t.setDaemon(true);
                    return t;
                });
                
                // 注意：Spring Cloud Alibaba 2.2.5 的 NacosServiceDiscovery 
                // 可能不直接支持监听，这里提供框架，实际实现需要根据版本调整
                logger.info("[NacosRegistry] Service change listener initialized (framework ready)");
                logger.warn("[NacosRegistry] Note: Listener implementation depends on Spring Cloud Alibaba version. " +
                        "If not supported, TTL cache will be used as fallback.");
            } catch (Exception e) {
                logger.warn("[NacosRegistry] Failed to initialize listener, will use TTL cache only", e);
                listenerInitialized.set(false);
            }
        }
    }
    
    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        if (listenerExecutor != null && !listenerExecutor.isShutdown()) {
            try {
                listenerExecutor.shutdown();
                if (!listenerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    listenerExecutor.shutdownNow();
                }
                logger.debug("[NacosRegistry] Listener executor shutdown completed");
            } catch (InterruptedException e) {
                listenerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 安全地更新缓存（线程安全）
     * 用于监听事件触发时更新缓存
     * 
     * @param serviceName 服务名称
     * @param instances 服务实例列表
     */
    private void updateCacheSafely(String serviceName, List<ServiceInstance> instances) {
        if (instanceCache == null || serviceName == null || serviceName.isEmpty()) {
            return;
        }
        
        try {
            // Caffeine 的 put 操作是线程安全的
            // 创建副本避免外部修改影响缓存
            List<ServiceInstance> instancesCopy = instances != null 
                ? new java.util.ArrayList<>(instances) 
                : Collections.emptyList();
            
            instanceCache.put(serviceName, instancesCopy);
            logger.debug("[NacosRegistry] Cache updated via listener for service: {}, instances: {}", 
                    serviceName, instancesCopy.size());
        } catch (Exception e) {
            logger.warn("[NacosRegistry] Failed to update cache for service: {}", serviceName, e);
            // 更新失败不影响主流程，依赖 TTL 机制
        }
    }
    
    /**
     * 处理服务变更事件（由监听器调用）
     * 这个方法可以在未来集成 Nacos 监听机制时使用
     * 
     * @param serviceName 服务名称
     */
    public void onServiceChange(String serviceName) {
        if (serviceName == null || serviceName.isEmpty() || listenerExecutor == null) {
            return;
        }
        
        // 异步更新缓存，避免阻塞监听线程
        listenerExecutor.submit(() -> {
            try {
                // 从 Nacos 重新获取最新实例列表
                List<org.springframework.cloud.client.ServiceInstance> nacosInstances = 
                        nacosServiceDiscovery != null 
                            ? nacosServiceDiscovery.getInstances(serviceName) 
                            : null;
                
                if (nacosInstances == null || nacosInstances.isEmpty()) {
                    updateCacheSafely(serviceName, Collections.emptyList());
                    return;
                }
                
                List<ServiceInstance> instances = nacosInstances.stream()
                        .map(this::convertToServiceInstance)
                        .filter(instance -> instance != null)
                        .collect(Collectors.toList());
                
                updateCacheSafely(serviceName, instances);
            } catch (Exception e) {
                logger.warn("[NacosRegistry] Failed to refresh cache for service: {} via listener", 
                        serviceName, e);
                // 监听更新失败不影响主流程，依赖 TTL 机制
            }
        });
    }

    @Override
    public List<ServiceInstance> listInstances(String serviceName) {
        if (nacosServiceDiscovery == null) {
            logger.warn("[NacosRegistry] NacosServiceDiscovery is not available");
            return Collections.emptyList();
        }

        if (serviceName == null || serviceName.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 如果缓存启用，先从缓存获取
        if (instanceCache != null) {
            List<ServiceInstance> cached = instanceCache.getIfPresent(serviceName);
            if (cached != null) {
                logger.debug("[NacosRegistry] Cache hit for service: {}, instances: {}", 
                        serviceName, cached.size());
                return new java.util.ArrayList<>(cached); // 返回副本，避免外部修改影响缓存
            }
        }

        // 缓存未命中或未启用，从 Nacos 获取
        try {
            List<org.springframework.cloud.client.ServiceInstance> nacosInstances = 
                    nacosServiceDiscovery.getInstances(serviceName);
            
            if (nacosInstances == null || nacosInstances.isEmpty()) {
                logger.debug("[NacosRegistry] No instances found for service: {}", serviceName);
                // 缓存空结果，避免频繁查询
                if (instanceCache != null) {
                    instanceCache.put(serviceName, Collections.emptyList());
                }
                return Collections.emptyList();
            }

            List<ServiceInstance> instances = nacosInstances.stream()
                    .map(this::convertToServiceInstance)
                    .filter(instance -> instance != null)
                    .collect(Collectors.toList());

            // 存入缓存
            if (instanceCache != null) {
                instanceCache.put(serviceName, new java.util.ArrayList<>(instances));
                logger.debug("[NacosRegistry] Cached {} instances for service: {}", 
                        instances.size(), serviceName);
            }

            logger.debug("[NacosRegistry] Found {} instances for service: {}", instances.size(), serviceName);
            return instances;
        } catch (Exception e) {
            logger.warn("[NacosRegistry] Failed to get instances from Nacos for service: {}", serviceName, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 刷新指定服务的缓存
     * 
     * @param serviceName 服务名称
     */
    public void refreshCache(String serviceName) {
        if (instanceCache != null && serviceName != null && !serviceName.isEmpty()) {
            instanceCache.invalidate(serviceName);
            logger.debug("[NacosRegistry] Cache invalidated for service: {}", serviceName);
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clearCache() {
        if (instanceCache != null) {
            instanceCache.invalidateAll();
            logger.debug("[NacosRegistry] All cache cleared");
        }
    }

    @Override
    public ServiceInstance current() {
        if (nacosDiscoveryProperties == null) {
            return null;
        }

        try {
            ServiceInstance instance = new ServiceInstance();
            instance.setServiceName(nacosDiscoveryProperties.getService());
            instance.setHost(nacosDiscoveryProperties.getIp());
            instance.setPort(nacosDiscoveryProperties.getPort());
            instance.setMetadata(nacosDiscoveryProperties.getMetadata());
            instance.setHealthy(true);
            
            // 获取 nodeId：优先从 metadata 中获取 instanceId，如果为空则使用 host:port 作为后备
            // 注意：spring-cloud-starter-alibaba-nacos-discovery 2.2.5 版本中 NacosDiscoveryProperties 没有 getInstanceId() 方法
            String nodeId = null;
            if (nacosDiscoveryProperties.getMetadata() != null) {
                nodeId = nacosDiscoveryProperties.getMetadata().get("instanceId");
            }
            // 如果仍然为空，使用 host:port 作为后备
            if (nodeId == null || nodeId.isEmpty()) {
                nodeId = nacosDiscoveryProperties.getIp() + ":" + nacosDiscoveryProperties.getPort();
            }
            instance.setNodeId(nodeId);
            
            return instance;
        } catch (Exception e) {
            logger.warn("[NacosRegistry] Failed to get current instance from Nacos", e);
            return null;
        }
    }

    @Override
    public boolean supports(String serviceName) {
        // Nacos 注册中心理论上支持所有服务
        // 但为了更精确的控制，可以通过配置来限制
        if (serviceName == null || serviceName.isEmpty()) {
            return false;
        }

        // 如果配置了服务白名单，只支持白名单中的服务
        if (properties != null && properties.getServiceWhitelist() != null 
                && !properties.getServiceWhitelist().isEmpty()) {
            return properties.getServiceWhitelist().contains(serviceName);
        }

        // 默认支持所有服务
        return true;
    }

    @Override
    public int getPriority() {
        if (properties != null && properties.getPriority() != null) {
            return properties.getPriority();
        }
        return DEFAULT_PRIORITY;
    }

    /**
     * 将 Spring Cloud ServiceInstance 转换为项目 ServiceInstance
     */
    private ServiceInstance convertToServiceInstance(org.springframework.cloud.client.ServiceInstance nacosInstance) {
        if (nacosInstance == null) {
            return null;
        }

        ServiceInstance instance = new ServiceInstance();
        instance.setServiceName(nacosInstance.getServiceId());
        instance.setHost(nacosInstance.getHost());
        instance.setPort(nacosInstance.getPort());
        instance.setMetadata(nacosInstance.getMetadata());
        // Nacos 的健康状态可以从 metadata 中获取，或者默认为 true
        boolean healthy = true;
        if (nacosInstance.getMetadata() != null && nacosInstance.getMetadata().containsKey("healthy")) {
            String healthyStr = nacosInstance.getMetadata().get("healthy");
            healthy = Boolean.parseBoolean(healthyStr);
        }
        instance.setHealthy(healthy);
        
        // Nacos 的实例 ID 可以从 metadata 中获取，或者使用 host:port 作为 nodeId
        String nodeId = null;
        if (nacosInstance.getMetadata() != null) {
            nodeId = nacosInstance.getMetadata().get("instanceId");
        }
        if (nodeId == null || nodeId.isEmpty()) {
            nodeId = nacosInstance.getHost() + ":" + nacosInstance.getPort();
        }
        instance.setNodeId(nodeId);
        
        return instance;
    }
}
