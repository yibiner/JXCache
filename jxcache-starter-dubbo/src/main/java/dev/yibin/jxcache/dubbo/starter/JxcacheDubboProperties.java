package dev.yibin.jxcache.dubbo.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dubbo consumer 侧缓存插件配置。
 */
@ConfigurationProperties(prefix = "jxc.dubbo")
public class JxcacheDubboProperties {

    /**
     * 是否启用 Dubbo consumer 侧 JetCache 包装器。
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
