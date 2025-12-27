package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 缓存一致性检查结果
 * 用于聚合查询多个节点的单个缓存条目时，检查各节点缓存数据是否一致
 */
public class CacheConsistencyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 缓存区域
     */
    private String area;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存键
     */
    private String key;

    /**
     * 是否一致
     * true: 所有节点的缓存值一致
     * false: 存在节点缓存值不一致或部分节点查询失败
     */
    private boolean consistent;

    /**
     * 各节点的缓存条目详情
     */
    private List<LocalCacheEntryDetail> entries;

    /**
     * 一致性检查详情
     * 描述不一致的原因或一致性的统计信息
     */
    private String consistencyDetail;

    /**
     * 查询失败的节点列表
     */
    private List<String> failedNodes;

    /**
     * 查询时间
     */
    private long queryTime;

    public CacheConsistencyResult() {
        this.queryTime = System.currentTimeMillis();
    }

    public CacheConsistencyResult(String area, String cacheName, String key) {
        this();
        this.area = area;
        this.cacheName = cacheName;
        this.key = key;
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

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public List<LocalCacheEntryDetail> getEntries() {
        return entries;
    }

    public void setEntries(List<LocalCacheEntryDetail> entries) {
        this.entries = entries;
    }

    public String getConsistencyDetail() {
        return consistencyDetail;
    }

    public void setConsistencyDetail(String consistencyDetail) {
        this.consistencyDetail = consistencyDetail;
    }

    public List<String> getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(List<String> failedNodes) {
        this.failedNodes = failedNodes;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(long queryTime) {
        this.queryTime = queryTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheConsistencyResult that = (CacheConsistencyResult) o;
        return consistent == that.consistent
                && queryTime == that.queryTime
                && Objects.equals(area, that.area)
                && Objects.equals(cacheName, that.cacheName)
                && Objects.equals(key, that.key)
                && Objects.equals(entries, that.entries)
                && Objects.equals(consistencyDetail, that.consistencyDetail)
                && Objects.equals(failedNodes, that.failedNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(area, cacheName, key, consistent, entries, consistencyDetail, failedNodes, queryTime);
    }

    @Override
    public String toString() {
        return "CacheConsistencyResult{" +
                "area='" + area + '\'' +
                ", cacheName='" + cacheName + '\'' +
                ", key='" + key + '\'' +
                ", consistent=" + consistent +
                ", entriesCount=" + (entries != null ? entries.size() : 0) +
                ", consistencyDetail='" + consistencyDetail + '\'' +
                ", failedNodesCount=" + (failedNodes != null ? failedNodes.size() : 0) +
                ", queryTime=" + queryTime +
                '}';
    }
}

