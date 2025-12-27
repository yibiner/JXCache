package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * 缓存失效结果
 */
public class InvalidateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 节点 ID（用于聚合场景）
     */
    private String nodeId;

    /**
     * 缓存区域
     */
    private String area;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存键（如果指定了）
     */
    private String key;

    /**
     * 是否清除了本地缓存
     */
    private boolean localInvalidated;

    /**
     * 是否清除了远程缓存（Redis）
     */
    private boolean remoteInvalidated;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 操作时间
     */
    private long operationTime;

    public InvalidateResult() {
        this.operationTime = System.currentTimeMillis();
    }

    public InvalidateResult(String area, String cacheName, String key) {
        this();
        this.area = area;
        this.cacheName = cacheName;
        this.key = key;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isLocalInvalidated() {
        return localInvalidated;
    }

    public void setLocalInvalidated(boolean localInvalidated) {
        this.localInvalidated = localInvalidated;
    }

    public boolean isRemoteInvalidated() {
        return remoteInvalidated;
    }

    public void setRemoteInvalidated(boolean remoteInvalidated) {
        this.remoteInvalidated = remoteInvalidated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(long operationTime) {
        this.operationTime = operationTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvalidateResult that = (InvalidateResult) o;
        return success == that.success
                && localInvalidated == that.localInvalidated
                && remoteInvalidated == that.remoteInvalidated
                && operationTime == that.operationTime
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(area, that.area)
                && Objects.equals(cacheName, that.cacheName)
                && Objects.equals(key, that.key)
                && Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, nodeId, area, cacheName, key, localInvalidated, remoteInvalidated, errorMessage, operationTime);
    }

    @Override
    public String toString() {
        return "InvalidateResult{" +
                "success=" + success +
                ", nodeId='" + nodeId + '\'' +
                ", area='" + area + '\'' +
                ", cacheName='" + cacheName + '\'' +
                ", key='" + key + '\'' +
                ", localInvalidated=" + localInvalidated +
                ", remoteInvalidated=" + remoteInvalidated +
                ", errorMessage='" + errorMessage + '\'' +
                ", operationTime=" + operationTime +
                '}';
    }
}

