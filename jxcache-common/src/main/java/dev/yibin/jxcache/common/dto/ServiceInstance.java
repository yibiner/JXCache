package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * 服务实例
 */
public class ServiceInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nodeId;

    private String serviceName;

    private String host;

    private int port;

    private boolean healthy = true;

    private Map<String, String> metadata;

    public ServiceInstance() {
    }

    public ServiceInstance(String nodeId, String serviceName, String host, int port) {
        this.nodeId = nodeId;
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getUrl() {
        return "http://" + host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return port == that.port
                && healthy == that.healthy
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(host, that.host)
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, serviceName, host, port, healthy, metadata);
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "nodeId='" + nodeId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", healthy=" + healthy +
                ", metadata=" + metadata +
                '}';
    }
}
