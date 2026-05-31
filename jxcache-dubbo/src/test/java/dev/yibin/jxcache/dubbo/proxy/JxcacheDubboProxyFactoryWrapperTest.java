package dev.yibin.jxcache.dubbo.proxy;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import dev.yibin.jxcache.dubbo.support.JxcacheDubboContextHolder;
import dev.yibin.jxcache.dubbo.testsupport.CachedUserService;
import dev.yibin.jxcache.dubbo.testsupport.PlainUserService;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JxcacheDubboProxyFactoryWrapper} 的核心场景测试。
 */
@SpringBootTest(
        classes = JxcacheDubboProxyFactoryWrapperTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "jetcache.statIntervalMinutes=15",
                "jetcache.areaInCacheName=false",
                "jetcache.local.default.type=caffeine",
                "jetcache.local.default.limit=100",
                "jetcache.local.default.expireAfterWriteInMillis=600000"
        }
)
class JxcacheDubboProxyFactoryWrapperTest {

    private static final String TEST_USER_ID = "u1";

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        JxcacheDubboContextHolder.setApplicationContext(applicationContext);
    }

    @AfterEach
    void tearDown() {
        JxcacheDubboContextHolder.clear();
    }

    @Test
    @DisplayName("未声明 @Cached 的接口应直接返回原始 Dubbo 代理")
    void shouldReturnOriginalDubboProxyWhenInterfaceDoesNotDeclareCachedMethod() {
        final PlainUserService dubboProxy = (PlainUserService) Proxy.newProxyInstance(
                PlainUserService.class.getClassLoader(),
                new Class<?>[]{PlainUserService.class},
                (proxy, method, args) -> "pong:" + args[0]);

        final ProxyFactory delegate = mock(ProxyFactory.class);
        @SuppressWarnings("unchecked")
        final Invoker<PlainUserService> invoker =
                mockInvoker(delegate, PlainUserService.class, dubboProxy);
        final JxcacheDubboProxyFactoryWrapper wrapper =
                new JxcacheDubboProxyFactoryWrapper(delegate);

        final PlainUserService actual = wrapper.getProxy(invoker);

        assertThat(actual).isSameAs(dubboProxy);
        assertThat(actual.ping(TEST_USER_ID)).isEqualTo("pong:" + TEST_USER_ID);
    }

    @Test
    @DisplayName("声明 @Cached 的默认方法应在 consumer 侧命中缓存，并只调用一次远端 no-cache 方法")
    void shouldCacheOnConsumerSideAndOnlyInvokeRemoteNoCacheMethodOnce() {
        final AtomicInteger remoteCounter = new AtomicInteger();
        final AtomicInteger pingCounter = new AtomicInteger();
        final CachedUserService dubboProxy = (CachedUserService) Proxy.newProxyInstance(
                CachedUserService.class.getClassLoader(),
                new Class<?>[]{CachedUserService.class},
                (proxy, method, args) -> {
                    if ("getUserNoCache".equals(method.getName())) {
                        return "remote:" + args[0] + ":" + remoteCounter.incrementAndGet();
                    }
                    if ("ping".equals(method.getName())) {
                        return "ping:" + args[0] + ":" + pingCounter.incrementAndGet();
                    }
                    if ("toString".equals(method.getName())) {
                        return "cached-dubbo-proxy";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException(method.toGenericString());
                });

        final ProxyFactory delegate = mock(ProxyFactory.class);
        @SuppressWarnings("unchecked")
        final Invoker<CachedUserService> invoker =
                mockInvoker(delegate, CachedUserService.class, dubboProxy);

        final JxcacheDubboProxyFactoryWrapper wrapper =
                new JxcacheDubboProxyFactoryWrapper(delegate);

        final CachedUserService actual = wrapper.getProxy(invoker);

        assertThat(AopUtils.isAopProxy(actual)).isTrue();
        assertThat(actual.getUser(TEST_USER_ID)).isEqualTo("remote:" + TEST_USER_ID + ":1");
        assertThat(actual.getUser(TEST_USER_ID)).isEqualTo("remote:" + TEST_USER_ID + ":1");
        assertThat(remoteCounter.get()).isEqualTo(1);

        assertThat(actual.ping(TEST_USER_ID)).isEqualTo("ping:" + TEST_USER_ID + ":1");
        assertThat(actual.ping(TEST_USER_ID)).isEqualTo("ping:" + TEST_USER_ID + ":2");
        assertThat(pingCounter.get()).isEqualTo(2);
    }

    private <T> Invoker<T> mockInvoker(ProxyFactory delegate, Class<T> interfaceType, T dubboProxy) {
        @SuppressWarnings("unchecked")
        final Invoker<T> invoker = mock(Invoker.class);
        when(invoker.getInterface()).thenReturn(interfaceType);
        when(delegate.getProxy(invoker, false)).thenReturn(dubboProxy);
        return invoker;
    }

    @SpringBootApplication(scanBasePackages = "dev.yibin.jxcache.dubbo")
    @EnableMethodCache(basePackages = "dev.yibin.jxcache.dubbo")
    static class TestApplication {
    }
}
