package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 本地缓存快照
 */
public class LocalCacheSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nodeId;

    private String area;

    private String cacheName;

    private List<CacheEntry> entries;

    private long total;

    private boolean partial;

    private long queryTime;

    /**
     * 扫描/查询层级。通常为 L0（本地缓存）。
     */
    private String level;

    /**
     * 友好提示信息（例如：远端缓存不支持列举 key）。
     */
    private String message;

    public LocalCacheSnapshot() {
        this.queryTime = System.currentTimeMillis();
    }

    public LocalCacheSnapshot(String nodeId, String area, String cacheName) {
        this();
        this.nodeId = nodeId;
        this.area = area;
        this.cacheName = cacheName;
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

    public List<CacheEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<CacheEntry> entries) {
        this.entries = entries;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(long queryTime) {
        this.queryTime = queryTime;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalCacheSnapshot that = (LocalCacheSnapshot) o;
        return total == that.total
                && partial == that.partial
                && queryTime == that.queryTime
                && Objects.equals(nodeId, that.nodeId)
                && Objects.equals(area, that.area)
                && Objects.equals(cacheName, that.cacheName)
                && Objects.equals(entries, that.entries)
                && Objects.equals(level, that.level)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, area, cacheName, entries, total, partial, queryTime, level, message);
    }

    @Override
    public String toString() {
        return "LocalCacheSnapshot{" +
                "nodeId='" + nodeId + '\'' +
                ", area='" + area + '\'' +
                ", cacheName='" + cacheName + '\'' +
                ", entries=" + entries +
                ", total=" + total +
                ", partial=" + partial +
                ", queryTime=" + queryTime +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
