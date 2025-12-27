package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * 缓存查询请求
 */
public class QueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String area;

    private String cacheName;

    private String keyPrefix;

    private PageRequest pageRequest;

    private int shard;

    private int totalShards;

    /**
     * 查询层级：
     * - L0: 本地缓存
     * - L1: 远端缓存（如 Redis）
     * - AUTO: 自动（由服务端决定）
     *
     * 说明：对于列表查询（scan/query），通常仅支持 L0；L1/AUTO 可能会被降级或返回提示信息。
     */
    private String level;

    public QueryRequest() {
    }

    public QueryRequest(String area, String cacheName, String keyPrefix, PageRequest pageRequest, int shard, int totalShards) {
        this.area = area;
        this.cacheName = cacheName;
        this.keyPrefix = keyPrefix;
        this.pageRequest = pageRequest;
        this.shard = shard;
        this.totalShards = totalShards;
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

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public PageRequest getPageRequest() {
        return pageRequest;
    }

    public void setPageRequest(PageRequest pageRequest) {
        this.pageRequest = pageRequest;
    }

    public int getShard() {
        return shard;
    }

    public void setShard(int shard) {
        this.shard = shard;
    }

    public int getTotalShards() {
        return totalShards;
    }

    public void setTotalShards(int totalShards) {
        this.totalShards = totalShards;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryRequest that = (QueryRequest) o;
        return shard == that.shard
                && totalShards == that.totalShards
                && Objects.equals(area, that.area)
                && Objects.equals(cacheName, that.cacheName)
                && Objects.equals(keyPrefix, that.keyPrefix)
                && Objects.equals(pageRequest, that.pageRequest)
                && Objects.equals(level, that.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(area, cacheName, keyPrefix, pageRequest, shard, totalShards, level);
    }

    @Override
    public String toString() {
        return "QueryRequest{" +
                "area='" + area + '\'' +
                ", cacheName='" + cacheName + '\'' +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", pageRequest=" + pageRequest +
                ", shard=" + shard +
                ", totalShards=" + totalShards +
                ", level='" + level + '\'' +
                '}';
    }
}
