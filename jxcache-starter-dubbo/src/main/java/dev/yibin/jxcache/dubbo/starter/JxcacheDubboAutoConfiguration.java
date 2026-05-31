package dev.yibin.jxcache.dubbo.starter;

import com.alicp.jetcache.anno.aop.CacheAdvisor;
import com.alicp.jetcache.anno.aop.JetCacheInterceptor;
import dev.yibin.jxcache.dubbo.support.JxcacheDubboApplicationContextBridge;
import org.apache.dubbo.rpc.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Dubbo consumer 侧 JetCache 插件自动配置。
 */
@Configuration
@ConditionalOnClass({ProxyFactory.class, CacheAdvisor.class, JetCacheInterceptor.class})
@ConditionalOnProperty(prefix = "jxc.dubbo", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JxcacheDubboProperties.class)
public class JxcacheDubboAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(JxcacheDubboAutoConfiguration.class);

    @PostConstruct
    public void init() {
        logger.info("[Dubbo] JXCache Dubbo consumer cache plugin enabled and initialized");
    }

    @Bean
    @ConditionalOnMissingBean
    public JxcacheDubboApplicationContextBridge jxcacheDubboApplicationContextBridge() {
        return new JxcacheDubboApplicationContextBridge();
    }
}
