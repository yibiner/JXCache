package dev.yibin.jxcache.observer.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ObserverAutoConfiguration 自动配置测试
 * 
 * 验证自动配置类的注解和结构是否正确
 */
class ObserverAutoConfigurationTest {

    @Test
    void testAutoConfigurationClassExists() {
        // 验证自动配置类存在
        assertThat(ObserverAutoConfiguration.class).isNotNull();
        assertThat(ObserverAutoConfiguration.class.getAnnotation(org.springframework.context.annotation.Configuration.class))
                .isNotNull();
        assertThat(ObserverAutoConfiguration.class.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class))
                .isNotNull();
        assertThat(ObserverAutoConfiguration.class.getAnnotation(org.springframework.boot.context.properties.EnableConfigurationProperties.class))
                .isNotNull();
    }
}

