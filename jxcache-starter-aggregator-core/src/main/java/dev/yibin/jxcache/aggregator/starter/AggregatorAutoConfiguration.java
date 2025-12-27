package dev.yibin.jxcache.aggregator.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Aggregator 自动配置
 */
@Configuration
@ConditionalOnProperty(prefix = "jxc.aggregator", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AggregatorProperties.class)
@ComponentScan(basePackages = {
    "dev.yibin.jxcache.aggregator",
    "dev.yibin.jxcache.registry.spi"  // 扫描 RegistryClientFactory
})
/**
 * AggregatorAutoConfiguration
 *
 * @author Yibin
 * @since 1.0.0
 */
public class AggregatorAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AggregatorAutoConfiguration.class);
    
    @PostConstruct
    public void init() {
        logger.info("[Aggregator] JXCache Aggregator module enabled and initialized");
    }
}
