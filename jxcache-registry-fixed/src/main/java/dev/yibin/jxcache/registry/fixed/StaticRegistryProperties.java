package dev.yibin.jxcache.registry.fixed;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 静态注册中心配置属性
 * @author Yibin
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "jxc.registry.fixed")
public class StaticRegistryProperties {
    
    /**
     * 是否启用固定注册中心
     * 默认启用，作为降级方案
     */
    private boolean enabled = true;
    
    /**
     * 优先级，数值越小优先级越高
     * 默认值 1000，作为降级方案使用
     */
    private Integer priority;
    
    /**
     * 服务列表
     */
    private List<ServiceConfig> services = new ArrayList<>();
    
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
    
    public List<ServiceConfig> getServices() {
        return services;
    }
    
    public void setServices(List<ServiceConfig> services) {
        this.services = services;
    }
    
    /**
     * 服务配置
     */
    public static class ServiceConfig {
        
        /**
         * 服务名称
         */
        private String name;
        
        /**
         * 节点列表
         */
        private List<NodeConfig> nodes = new ArrayList<>();
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<NodeConfig> getNodes() {
            return nodes;
        }
        
        public void setNodes(List<NodeConfig> nodes) {
            this.nodes = nodes;
        }
    }
    
    /**
     * 节点配置
     */
    public static class NodeConfig {
        
        /**
         * 节点ID
         */
        private String nodeId;
        
        /**
         * 主机地址
         */
        private String host;
        
        /**
         * 端口
         */
        private int port;
        
        /**
         * 是否健康
         */
        private boolean healthy = true;
        
        /**
         * 元数据
         */
        private java.util.Map<String, String> metadata;
        
        public String getNodeId() {
            return nodeId;
        }
        
        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
        
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public java.util.Map<String, String> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(java.util.Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }
}
