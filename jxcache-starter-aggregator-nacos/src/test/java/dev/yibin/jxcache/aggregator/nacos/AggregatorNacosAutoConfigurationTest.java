package dev.yibin.jxcache.aggregator.nacos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AggregatorNacosAutoConfiguration 自动配置测试
 * 
 * 验证自动配置类的注解和结构是否正确
 */
class AggregatorNacosAutoConfigurationTest {

    @Test
    void testAutoConfigurationClassExists() {
        // 验证自动配置类存在
        assertThat(AggregatorNacosAutoConfiguration.class).isNotNull();
        assertThat(AggregatorNacosAutoConfiguration.class.getAnnotation(org.springframework.context.annotation.Configuration.class))
                .isNotNull();
        assertThat(AggregatorNacosAutoConfiguration.class.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class))
                .isNotNull();
        assertThat(AggregatorNacosAutoConfiguration.class.getAnnotation(org.springframework.context.annotation.ComponentScan.class))
                .isNotNull();
    }
}

