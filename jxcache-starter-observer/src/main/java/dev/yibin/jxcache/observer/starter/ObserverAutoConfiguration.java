package dev.yibin.jxcache.observer.starter;

import dev.yibin.jxcache.common.impl.DefaultAccessGuard;
import dev.yibin.jxcache.common.impl.DefaultRateLimiter;
import dev.yibin.jxcache.common.impl.DefaultValuePreviewer;
import dev.yibin.jxcache.common.spi.AccessGuard;
import dev.yibin.jxcache.common.spi.RateLimiter;
import dev.yibin.jxcache.common.spi.ValuePreviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Observer 自动配置
 * @author Yibin
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "jxc.observer", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObserverProperties.class)
@ComponentScan(basePackages = "dev.yibin.jxcache.observer")
public class ObserverAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(ObserverAutoConfiguration.class);
    
    @PostConstruct
    public void init() {
        logger.info("[Observer] JXCache Observer module enabled and initialized");
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "jxc.observer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ValuePreviewer valuePreviewer(ObserverProperties properties) {
        return new DefaultValuePreviewer();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "jxc.observer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AccessGuard accessGuard() {
        return new DefaultAccessGuard();
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "jxc.observer", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RateLimiter rateLimiter() {
        return new DefaultRateLimiter();
    }
}
