package dev.yibin.jxcache.dubbo.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 在 Spring 容器启动完成后，把 {@link ApplicationContext} 同步到静态上下文桥接器。
 */
public class JxcacheDubboApplicationContextBridge
        implements ApplicationContextAware, DisposableBean {

    private static final Logger logger =
            LoggerFactory.getLogger(JxcacheDubboApplicationContextBridge.class);

    /**
     * ApplicationContextAware 回调。
     *
     * @param applicationContext Spring 上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        JxcacheDubboContextHolder.setApplicationContext(applicationContext);
        logger.info("[JXCache-Dubbo] Spring application context initialized");
    }

    /**
     * Bean 销毁时清理静态上下文，避免测试或重复启动场景下遗留旧引用。
     */
    @Override
    public void destroy() {
        JxcacheDubboContextHolder.clear();
    }
}
