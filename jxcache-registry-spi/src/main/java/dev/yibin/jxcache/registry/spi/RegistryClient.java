package dev.yibin.jxcache.registry.spi;

import dev.yibin.jxcache.common.dto.ServiceInstance;

import java.util.List;

/**
 * 注册中心客户端 SPI 接口
 * <p>
 * 实现此接口以提供不同的服务发现机制，如：
 * - FixedRegistryClient: 基于配置文件的静态服务发现
 * - NacosRegistryClient: 基于 Nacos 注册中心的服务发现
 * - 其他自定义实现（如 Zookeeper、Consul 等）
 * @author Yibin
 * @since 1.0.0
 */
public interface RegistryClient {
    
    /**
     * 列出服务实例
     * 
     * @param serviceName 服务名称
     * @return 服务实例列表
     */
    List<ServiceInstance> listInstances(String serviceName);
    
    /**
     * 获取当前实例
     * 
     * @return 当前服务实例
     */
    ServiceInstance current();
    
    /**
     * 是否支持该服务
     * 
     * @param serviceName 服务名称
     * @return 是否支持
     */
    boolean supports(String serviceName);
    
    /**
     * 获取客户端优先级
     * 数值越小优先级越高，默认返回 Integer.MAX_VALUE
     * 
     * 当多个 RegistryClient 都支持同一服务时，优先使用优先级高的客户端
     * 
     * @return 优先级值
     */
    default int getPriority() {
        return Integer.MAX_VALUE;
    }
    
    /**
     * 获取客户端名称，用于日志和调试
     * 
     * @return 客户端名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
