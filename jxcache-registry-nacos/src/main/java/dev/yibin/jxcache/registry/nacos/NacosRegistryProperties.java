package dev.yibin.jxcache.registry.nacos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Nacos 注册中心配置属性
 * @author Yibin
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jxc.registry.nacos")
public class NacosRegistryProperties {
    
    /**
     * 是否启用 Nacos 注册中心
     * 默认启用
     */
    private boolean enabled = true;
    
    /**
     * 优先级，数值越小优先级越高
     * 默认值 100，优先于 FixedRegistryClient 使用
     */
    private Integer priority;
    
    /**
     * 服务白名单
     * 如果配置了白名单，只支持白名单中的服务
     * 如果为空，则支持所有服务
     */
    private List<String> serviceWhitelist = new ArrayList<>();
    
    /**
     * 服务实例列表缓存配置
     */
    private Cache cache = new Cache();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public List<String> getServiceWhitelist() {
        return serviceWhitelist;
    }
    
    public void setServiceWhitelist(List<String> serviceWhitelist) {
        this.serviceWhitelist = serviceWhitelist;
    }
    
    public Cache getCache() {
        return cache;
    }
    
    public void setCache(Cache cache) {
        this.cache = cache != null ? cache : new Cache();
    }
    
    /**
     * 缓存配置
     */
    public static class Cache {
        
        /**
         * 是否启用缓存
         * 默认启用
         */
        private boolean enabled = true;
        
        /**
         * 缓存过期时间（秒）
         * 默认 10 秒
         */
        private int expireSeconds = 10;
        
        /**
         * 最大缓存条目数
         * 默认 1000
         */
        private int maxSize = 1000;
        
        /**
         * 是否启用服务变更监听
         * 默认禁用（使用 TTL 机制即可满足大多数场景）
         * 启用后，会监听 Nacos 服务变更事件并实时更新缓存
         */
        private boolean enableListener = false;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getExpireSeconds() {
            return expireSeconds;
        }
        
        public void setExpireSeconds(int expireSeconds) {
            this.expireSeconds = expireSeconds > 0 ? expireSeconds : 10;
        }
        
        public int getMaxSize() {
            return maxSize;
        }
        
        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize > 0 ? maxSize : 1000;
        }
        
        public boolean isEnableListener() {
            return enableListener;
        }
        
        public void setEnableListener(boolean enableListener) {
            this.enableListener = enableListener;
        }
    }
}
