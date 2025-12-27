package dev.yibin.jxcache.aggregator.nacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 聚合模块与 Nacos 的自动配置
 * 
 * 同时支持：
 * - NacosRegistryClient：从 Nacos 注册中心获取服务实例（优先级高）
 * - StaticRegistryClient：从配置文件获取服务实例（优先级低，作为降级方案）
 * 
 * 当 Nacos 注册中心不可用时，自动降级到固定配置
 * @author Yibin
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "jxc.aggregator", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = {
    "dev.yibin.jxcache.registry.nacos",
    "dev.yibin.jxcache.registry.fixed",
    "dev.yibin.jxcache.registry.spi"  // 扫描注册中心客户端工厂
})
public class AggregatorNacosAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AggregatorNacosAutoConfiguration.class);
    
    @PostConstruct
    public void init() {
        logger.info("[Aggregator] JXCache Aggregator with Nacos registry enabled and initialized");
    }
}
