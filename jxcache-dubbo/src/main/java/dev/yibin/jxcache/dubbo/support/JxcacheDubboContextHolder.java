package dev.yibin.jxcache.dubbo.support;

import org.springframework.context.ApplicationContext;

/**
 * 保存 Dubbo SPI 包装器访问 Spring 容器所需的 {@link ApplicationContext}。
 * <p>
 * Dubbo 通过 SPI 直接实例化包装器，无法像普通 Spring Bean 一样完成依赖注入，
 * 因此这里通过静态上下文做一次轻量桥接。
 * </p>
 */
public final class JxcacheDubboContextHolder {

    private static volatile ApplicationContext applicationContext;

    private JxcacheDubboContextHolder() {
    }

    /**
     * 获取当前 Spring 上下文。
     *
     * @return Spring 上下文；未初始化时返回 {@code null}
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 设置当前 Spring 上下文。
     *
     * @param applicationContext Spring 上下文
     */
    public static void setApplicationContext(ApplicationContext applicationContext) {
        JxcacheDubboContextHolder.applicationContext = applicationContext;
    }

    /**
     * 清理静态持有的 Spring 上下文引用。
     */
    public static void clear() {
        JxcacheDubboContextHolder.applicationContext = null;
    }
}
