package dev.yibin.jxcache.dubbo.starter;

import dev.yibin.jxcache.dubbo.support.JxcacheDubboApplicationContextBridge;
import org.apache.dubbo.rpc.ProxyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JxcacheDubboAutoConfiguration} 的自动配置测试。
 */
class JxcacheDubboAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JxcacheDubboAutoConfiguration.class));

    @Test
    @DisplayName("默认开启时应注册 Dubbo 上下文桥接 Bean")
    void shouldRegisterContextBridgeWhenPluginIsEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JxcacheDubboProperties.class);
            assertThat(context.getBean(JxcacheDubboProperties.class).isEnabled()).isTrue();
            assertThat(context).hasSingleBean(JxcacheDubboApplicationContextBridge.class);
        });
    }

    @Test
    @DisplayName("显式禁用时不应注册 Dubbo 上下文桥接 Bean")
    void shouldNotRegisterContextBridgeWhenPluginIsDisabled() {
        contextRunner
                .withPropertyValues("jxc.dubbo.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JxcacheDubboProperties.class);
                    assertThat(context).doesNotHaveBean(JxcacheDubboApplicationContextBridge.class);
                });
    }

    @Test
    @DisplayName("缺少 Dubbo ProxyFactory 类时应回退自动配置")
    void shouldBackOffWhenDubboProxyFactoryClassIsMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ProxyFactory.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JxcacheDubboProperties.class);
                    assertThat(context).doesNotHaveBean(JxcacheDubboApplicationContextBridge.class);
                });
    }
}
