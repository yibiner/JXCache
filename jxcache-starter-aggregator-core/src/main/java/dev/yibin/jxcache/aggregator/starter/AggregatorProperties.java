package dev.yibin.jxcache.aggregator.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Aggregator 配置属性
 * @author Yibin
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jxc.aggregator")
public class AggregatorProperties {
    
    /**
     * 是否启用 Aggregator
     */
    private boolean enabled = true;
    
    /**
     * 单节点超时时间（毫秒）
     */
    private int perNodeTimeoutMs = 2000;
    
    /**
     * 总超时时间（毫秒）
     */
    private int totalTimeoutMs = 4000;
    
    /**
     * 最大并发数
     */
    private int maxConcurrency = 16;
    
    /**
     * 最大分页大小
     */
    private int maxPageSize = 200;
    
    /**
     * Observer 扫描路径
     */
    private String observerScanPath = "/api/jxc/observer/query";
    
    /**
     * Observer 条目查询路径
     */
    private String observerEntryPath = "/api/jxc/observer/entry";
    
    /**
     * Observer 失效缓存路径
     */
    private String observerInvalidatePath = "/api/jxc/observer/invalidate";
    
    /**
     * HTTP 客户端配置
     */
    private Http http = new Http();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getPerNodeTimeoutMs() {
        return perNodeTimeoutMs;
    }
    
    public void setPerNodeTimeoutMs(int perNodeTimeoutMs) {
        this.perNodeTimeoutMs = perNodeTimeoutMs;
    }
    
    public int getTotalTimeoutMs() {
        return totalTimeoutMs;
    }
    
    public void setTotalTimeoutMs(int totalTimeoutMs) {
        this.totalTimeoutMs = totalTimeoutMs;
    }
    
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
    
    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
    
    public int getMaxPageSize() {
        return maxPageSize;
    }
    
    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
    
    public String getObserverScanPath() {
        return observerScanPath;
    }
    
    public void setObserverScanPath(String observerScanPath) {
        this.observerScanPath = observerScanPath;
    }
    
    public String getObserverEntryPath() {
        return observerEntryPath;
    }
    
    public void setObserverEntryPath(String observerEntryPath) {
        this.observerEntryPath = observerEntryPath;
    }
    
    public String getObserverInvalidatePath() {
        return observerInvalidatePath;
    }
    
    public void setObserverInvalidatePath(String observerInvalidatePath) {
        this.observerInvalidatePath = observerInvalidatePath;
    }
    
    public Http getHttp() {
        return http;
    }
    
    public void setHttp(Http http) {
        this.http = http != null ? http : new Http();
    }
    
    /**
     * HTTP 客户端配置
     */
    public static class Http {
        
        /**
         * 连接超时时间（毫秒）
         * 默认 5000ms
         */
        private int connectTimeoutMs = 5000;
        
        /**
         * 读取超时时间（毫秒）
         * 默认 10000ms
         */
        private int readTimeoutMs = 10000;
        
        /**
         * 从连接池获取连接的超时时间（毫秒）
         * 默认 2000ms
         */
        private int connectionRequestTimeoutMs = 2000;
        
        /**
         * 最大连接数
         * 默认 200
         */
        private int maxTotal = 200;
        
        /**
         * 每个路由的最大连接数
         * 默认 50
         */
        private int maxPerRoute = 50;
        
        /**
         * 空闲连接清理时间（秒）
         * 默认 30 秒
         */
        private int evictIdleConnectionsSeconds = 30;
        
        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }
        
        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs > 0 ? connectTimeoutMs : 5000;
        }
        
        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }
        
        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs > 0 ? readTimeoutMs : 10000;
        }
        
        public int getConnectionRequestTimeoutMs() {
            return connectionRequestTimeoutMs;
        }
        
        public void setConnectionRequestTimeoutMs(int connectionRequestTimeoutMs) {
            this.connectionRequestTimeoutMs = connectionRequestTimeoutMs > 0 
                ? connectionRequestTimeoutMs : 2000;
        }
        
        public int getMaxTotal() {
            return maxTotal;
        }
        
        public void setMaxTotal(int maxTotal) {
            this.maxTotal = maxTotal > 0 ? maxTotal : 200;
        }
        
        public int getMaxPerRoute() {
            return maxPerRoute;
        }
        
        public void setMaxPerRoute(int maxPerRoute) {
            this.maxPerRoute = maxPerRoute > 0 ? maxPerRoute : 50;
        }
        
        public int getEvictIdleConnectionsSeconds() {
            return evictIdleConnectionsSeconds;
        }
        
        public void setEvictIdleConnectionsSeconds(int evictIdleConnectionsSeconds) {
            this.evictIdleConnectionsSeconds = evictIdleConnectionsSeconds > 0 
                ? evictIdleConnectionsSeconds : 30;
        }
    }
}
