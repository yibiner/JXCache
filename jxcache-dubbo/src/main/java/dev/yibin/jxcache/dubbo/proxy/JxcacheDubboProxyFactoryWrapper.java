package dev.yibin.jxcache.dubbo.proxy;

import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.aop.CacheAdvisor;
import com.alicp.jetcache.anno.aop.CachePointcut;
import com.alicp.jetcache.anno.aop.JetCacheInterceptor;
import dev.yibin.jxcache.dubbo.support.JxcacheDubboContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo consumer 侧的 JetCache 代理包装器。
 * <ol>
 *     <li>仅拦截声明了 {@link Cached} 的接口方法，普通 Dubbo 方法保持原有调用路径。</li>
 *     <li>缓存未命中时执行接口 default 方法，保证缓存逻辑在 consumer 侧完成。</li>
 *     <li>default 方法内部再转调 no-cache 方法，从而只触发一次远端 RPC。</li>
 * </ol>
 */
public class JxcacheDubboProxyFactoryWrapper implements ProxyFactory {

    private static final Logger logger =
            LoggerFactory.getLogger(JxcacheDubboProxyFactoryWrapper.class);
    private static final String LOG_PREFIX = "[JXCache-Dubbo]";

    private final ProxyFactory proxyFactory;

    private volatile CacheAdvisor cacheAdvisor;
    private volatile JetCacheInterceptor jetCacheInterceptor;

    /**
     * Dubbo SPI 会把真实的 {@link ProxyFactory} 实现作为包装器构造参数传入。
     *
     * @param proxyFactory Dubbo 原始代理工厂
     */
    public JxcacheDubboProxyFactoryWrapper(ProxyFactory proxyFactory) {
        this.proxyFactory = Objects.requireNonNull(proxyFactory, "proxyFactory");
    }

    /**
     * 为 Dubbo consumer 代理补上一层 JetCache AOP 拦截。
     *
     * @param invoker Dubbo invoker
     * @param generic 是否泛化调用
     * @param <T>     业务接口类型
     * @return 优先带有 JetCache 能力的代理对象；不满足条件时回退到原始 Dubbo 代理
     * @throws RpcException Dubbo 代理创建异常
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
        if (invoker == null) {
            return null;
        }

        final Class<?> interfaceClass = invoker.getInterface();
        final T dubboProxy = proxyFactory.getProxy(invoker, generic);
        if (dubboProxy == null || interfaceClass == null) {
            return dubboProxy;
        }

        if (!interfaceClass.isInterface()) {
            return dubboProxy;
        }

        final CachePointcut cachePointcut = getCachePointcut();
        final JetCacheInterceptor interceptor = getJetCacheInterceptor();
        if (cachePointcut == null || interceptor == null) {
            return dubboProxy;
        }

        final org.springframework.aop.framework.ProxyFactory defaultProxyFactory =
                new org.springframework.aop.framework.ProxyFactory();
        defaultProxyFactory.setInterfaces(interfaceClass);
        defaultProxyFactory.setTarget(dubboProxy);
        defaultProxyFactory.addAdvisor(new DefaultPointcutAdvisor(
                new StaticMethodMatcherPointcut() {
                    @Override
                    public boolean matches(Method method, Class<?> targetClass) {
                        return isCachedDefaultMethod(method);
                    }
                },
                new CachedDefaultMethodInterceptor(dubboProxy, interfaceClass)));
        final T defaultProxy = (T) defaultProxyFactory.getProxy();

        final org.springframework.aop.framework.ProxyFactory cacheProxyFactory =
                new org.springframework.aop.framework.ProxyFactory();
        cacheProxyFactory.setInterfaces(interfaceClass);
        cacheProxyFactory.setTarget(defaultProxy);
        cacheProxyFactory.addAdvisor(new DefaultPointcutAdvisor(
                new StaticMethodMatcherPointcut() {
                    @Override
                    public boolean matches(Method method, Class<?> targetClass) {
                        return isCachedMethod(method) && cachePointcut.matches(method, targetClass);
                    }
                },
                interceptor));
        final T cacheProxy = (T) cacheProxyFactory.getProxy();

        return AopUtils.canApply(cachePointcut, cacheProxy.getClass())
                ? cacheProxy
                : dubboProxy;
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return getProxy(invoker, false);
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return proxyFactory.getInvoker(proxy, type, url);
    }

    private CachePointcut getCachePointcut() {
        final CacheAdvisor advisor = getCacheAdvisor();
        if (advisor == null) {
            return null;
        }

        final Pointcut pointcut = advisor.getPointcut();
        if (pointcut instanceof CachePointcut) {
            return (CachePointcut) pointcut;
        }
        if (pointcut == null) {
            logger.warn("{} CacheAdvisor pointcut is null", LOG_PREFIX);
            return null;
        }
        logger.warn("{} CacheAdvisor pointcut is not CachePointcut: {}",
                LOG_PREFIX, pointcut.getClass().getName());
        return null;
    }

    private CacheAdvisor getCacheAdvisor() {
        if (cacheAdvisor != null) {
            return cacheAdvisor;
        }
        cacheAdvisor = getSpringBean(CacheAdvisor.class);
        return cacheAdvisor;
    }

    private JetCacheInterceptor getJetCacheInterceptor() {
        if (jetCacheInterceptor != null) {
            return jetCacheInterceptor;
        }
        jetCacheInterceptor = getSpringBean(JetCacheInterceptor.class);
        return jetCacheInterceptor;
    }

    private <T> T getSpringBean(Class<T> beanType) {
        final ApplicationContext applicationContext =
                JxcacheDubboContextHolder.getApplicationContext();
        if (applicationContext == null) {
            logger.debug("{} Spring ApplicationContext is not ready, skip Dubbo cache wrapper",
                    LOG_PREFIX);
            return null;
        }

        try {
            return applicationContext.getBean(beanType);
        } catch (BeansException ex) {
            logger.warn("{} Required bean '{}' is not found, skip Dubbo cache wrapper",
                    LOG_PREFIX, beanType.getSimpleName());
            return null;
        }
    }

    private static boolean isCachedMethod(Method method) {
        return method != null && method.getAnnotation(Cached.class) != null;
    }

    private static boolean isCachedDefaultMethod(Method method) {
        return isCachedMethod(method)
                && method.isDefault()
                && method.getDeclaringClass().isInterface();
    }

    /**
     * 执行接口 default 方法。
     * <p>
     * Dubbo 原始代理只负责发起远端调用；当我们希望 consumer 侧先执行接口默认实现时，
     * 需要显式使用 {@link MethodHandles.Lookup#unreflectSpecial(Method, Class)} 调用 default 方法。
     * </p>
     */
    private static final class CachedDefaultMethodInterceptor implements MethodInterceptor {

        private static final int LOOKUP_ALL_MODES = MethodHandles.Lookup.PUBLIC
                | MethodHandles.Lookup.PRIVATE
                | MethodHandles.Lookup.PROTECTED
                | MethodHandles.Lookup.PACKAGE;

        private final Object dubboProxy;
        private final String interfaceName;
        private final Map<Method, MethodHandle> methodHandleCache =
                new ConcurrentHashMap<Method, MethodHandle>(16);

        private CachedDefaultMethodInterceptor(Object dubboProxy, Class<?> serviceInterface) {
            this.dubboProxy = dubboProxy;
            this.interfaceName = serviceInterface.getSimpleName();
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            final Method method = invocation.getMethod();
            final MethodHandle methodHandle = methodHandleCache.computeIfAbsent(method,
                    key -> resolveDefaultMethodHandle(key, dubboProxy));
            logger.debug("{} Invoke cached default method '{}.{}'",
                    LOG_PREFIX, interfaceName, method.getName());
            return methodHandle.invokeWithArguments(invocation.getArguments());
        }

        private MethodHandle resolveDefaultMethodHandle(Method method, Object target) {
            try {
                final Constructor<MethodHandles.Lookup> constructor =
                        MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }
                final Class<?> declaringClass = method.getDeclaringClass();
                // JDK 8 环境下调用接口 default 方法，需要通过 Lookup#unreflectSpecial
                // 显式绑定到 Dubbo 原始代理，否则调用会再次落入代理分发链路。
                return constructor.newInstance(declaringClass, LOOKUP_ALL_MODES)
                        .unreflectSpecial(method, declaringClass)
                        .bindTo(target);
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Failed to invoke default method: " + method.toGenericString(), ex);
            }
        }
    }
}
