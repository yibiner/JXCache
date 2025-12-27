package dev.yibin.jxcache.aggregator.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * 聚合器配置
 */
@Configuration
public class AggregatorConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AggregatorConfig.class);
    
    // HTTP 配置默认值
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10000;
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS = 2000;
    private static final int DEFAULT_MAX_TOTAL = 200;
    private static final int DEFAULT_MAX_PER_ROUTE = 50;
    private static final int DEFAULT_EVICT_IDLE_CONNECTIONS_SECONDS = 30;
    
    @Value("${jxc.aggregator.http.connectTimeoutMs:#{null}}")
    private Integer connectTimeoutMs;
    
    @Value("${jxc.aggregator.http.readTimeoutMs:#{null}}")
    private Integer readTimeoutMs;
    
    @Value("${jxc.aggregator.http.connectionRequestTimeoutMs:#{null}}")
    private Integer connectionRequestTimeoutMs;
    
    @Value("${jxc.aggregator.http.maxTotal:#{null}}")
    private Integer maxTotal;
    
    @Value("${jxc.aggregator.http.maxPerRoute:#{null}}")
    private Integer maxPerRoute;
    
    @Value("${jxc.aggregator.http.evictIdleConnectionsSeconds:#{null}}")
    private Integer evictIdleConnectionsSeconds;
    
    private CloseableHttpClient httpClient;
    
    /**
     * 创建 RestTemplate Bean
     * 使用 Apache HttpClient 4.x 作为底层实现，支持连接池
     * 
     * @return RestTemplate 实例
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        try {
            // 获取 HTTP 配置，如果为空则使用默认值
            int connectTimeout = getConnectTimeoutMs();
            int readTimeout = getReadTimeoutMs();
            int connectionRequestTimeout = getConnectionRequestTimeoutMs();
            int maxTotalConnections = getMaxTotal();
            int maxPerRouteConnections = getMaxPerRoute();
            int evictIdleSeconds = getEvictIdleConnectionsSeconds();
            
            // 创建连接池管理器
            PoolingHttpClientConnectionManager connectionManager = 
                new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(maxTotalConnections);
            connectionManager.setDefaultMaxPerRoute(maxPerRouteConnections);
            
            // 配置请求参数
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(readTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build();
            
            // 创建 HttpClient
            httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(evictIdleSeconds, TimeUnit.SECONDS)
                .evictExpiredConnections()
                .build();
            
            // 创建 RestTemplate
            HttpComponentsClientHttpRequestFactory factory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);
            
            RestTemplate restTemplate = new RestTemplate(factory);
            
            logger.debug("[Aggregator] RestTemplate configured with HttpClient connection pool: " +
                "maxTotal={}, maxPerRoute={}, connectTimeout={}ms, readTimeout={}ms",
                maxTotalConnections, maxPerRouteConnections, connectTimeout, readTimeout);
            
            return restTemplate;
            
        } catch (Exception e) {
            logger.warn("[Aggregator] Failed to create RestTemplate with HttpClient, " +
                "falling back to default RestTemplate", e);
            // 降级方案：返回默认的 RestTemplate
            return new RestTemplate();
        }
    }
    
    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        if (httpClient != null) {
            try {
                httpClient.close();
                logger.debug("[Aggregator] HttpClient closed successfully");
            } catch (Exception e) {
                logger.warn("[Aggregator] Error closing HttpClient", e);
            }
        }
    }
    
    /**
     * 获取连接超时时间（毫秒）
     * 默认值：5000ms
     */
    private int getConnectTimeoutMs() {
        if (connectTimeoutMs != null && connectTimeoutMs > 0) {
            return connectTimeoutMs;
        }
        return DEFAULT_CONNECT_TIMEOUT_MS;
    }
    
    /**
     * 获取读取超时时间（毫秒）
     * 默认值：10000ms
     */
    private int getReadTimeoutMs() {
        if (readTimeoutMs != null && readTimeoutMs > 0) {
            return readTimeoutMs;
        }
        return DEFAULT_READ_TIMEOUT_MS;
    }
    
    /**
     * 获取连接请求超时时间（毫秒）
     * 默认值：2000ms
     */
    private int getConnectionRequestTimeoutMs() {
        if (connectionRequestTimeoutMs != null && connectionRequestTimeoutMs > 0) {
            return connectionRequestTimeoutMs;
        }
        return DEFAULT_CONNECTION_REQUEST_TIMEOUT_MS;
    }
    
    /**
     * 获取最大连接数
     * 默认值：200
     */
    private int getMaxTotal() {
        if (maxTotal != null && maxTotal > 0) {
            return maxTotal;
        }
        return DEFAULT_MAX_TOTAL;
    }
    
    /**
     * 获取每个路由的最大连接数
     * 默认值：50
     */
    private int getMaxPerRoute() {
        if (maxPerRoute != null && maxPerRoute > 0) {
            return maxPerRoute;
        }
        return DEFAULT_MAX_PER_ROUTE;
    }
    
    /**
     * 获取空闲连接清理时间（秒）
     * 默认值：30 秒
     */
    private int getEvictIdleConnectionsSeconds() {
        if (evictIdleConnectionsSeconds != null && evictIdleConnectionsSeconds > 0) {
            return evictIdleConnectionsSeconds;
        }
        return DEFAULT_EVICT_IDLE_CONNECTIONS_SECONDS;
    }
}
