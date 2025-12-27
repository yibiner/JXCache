package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * 单个本地缓存条目的完整视图
 */
public class LocalCacheEntryDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nodeId;

    private String area;

    private String cacheName;

    private String key;

    private String value;

    private String valueType;

    private int valueLength;

    private boolean truncated;

    private long queryTime;

    /**
     * 请求层级：
     * - L0: 本地缓存
     * - L1: 远端缓存（如 Redis）
     * - AUTO: 自动
     */
    private String requestedLevel;

    /**
     * 命中层级：
     * - L0: 命中本地缓存
     * - L1: 命中远端缓存
     * - UNKNOWN: 无法判断
     */
    private String hitLevel;

    public LocalCacheEntryDetail() {
        this.queryTime = System.currentTimeMillis();
    }

    public LocalCacheEntryDetail(String nodeId, String area, String cacheName) {
        this();
        this.nodeId = nodeId;
        this.area = area;
        this.cacheName = cacheName;
        this.truncated = false;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public int getValueLength() {
        return valueLength;
    }

    public void setValueLength(int valueLength) {
        this.valueLength = valueLength;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(long queryTime) {
        this.queryTime = queryTime;
    }

    public String getRequestedLevel() {
        return requestedLevel;
    }

    public void setRequestedLevel(String requestedLevel) {
        this.requestedLevel = requestedLevel;
    }

    public String getHitLevel() {
        return hitLevel;
    }

    public void setHitLevel(String hitLevel) {
        this.hitLevel = hitLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalCacheEntryDetail that = (LocalCacheEntryDetail) o;
        return valueLength == that.valueLength
                && truncated == that.truncated
                && queryTime == that.queryTime
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(area, that.area)
                && Objects.equals(cacheName, that.cacheName)
                && Objects.equals(key, that.key)
                && Objects.equals(value, that.value)
                && Objects.equals(valueType, that.valueType)
                && Objects.equals(requestedLevel, that.requestedLevel)
                && Objects.equals(hitLevel, that.hitLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, area, cacheName, key, value, valueType, valueLength, truncated, queryTime, requestedLevel, hitLevel);
    }

    @Override
    public String toString() {
        return "LocalCacheEntryDetail{" +
                "nodeId='" + nodeId + '\'' +
                ", area='" + area + '\'' +
                ", cacheName='" + cacheName + '\'' +
                ", key='" + key + '\'' +
                ", valueLength=" + valueLength +
                ", truncated=" + truncated +
                ", queryTime=" + queryTime +
                ", requestedLevel='" + requestedLevel + '\'' +
                ", hitLevel='" + hitLevel + '\'' +
                '}';
    }
}

