package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * 缓存失效请求
 */
public class InvalidateRequest implements Serializable {

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
     * 缓存键（可选，为空则清除整个缓存）
     */
    private String key;

    /**
     * 是否同时清除 Redis 缓存
     * 默认 false，只清除本地缓存
     */
    private boolean invalidateRemote = false;

    public InvalidateRequest() {
    }

    public InvalidateRequest(String area, String cacheName, String key) {
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

    public boolean isInvalidateRemote() {
        return invalidateRemote;
    }

    public void setInvalidateRemote(boolean invalidateRemote) {
        this.invalidateRemote = invalidateRemote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvalidateRequest that = (InvalidateRequest) o;
        return invalidateRemote == that.invalidateRemote
                && Objects.equals(area, that.area)
                && Objects.equals(cacheName, that.cacheName)
                && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(area, cacheName, key, invalidateRemote);
    }

    @Override
    public String toString() {
        return "InvalidateRequest{" +
                "area='" + area + '\'' +
                ", cacheName='" + cacheName + '\'' +
                ", key='" + key + '\'' +
                ", invalidateRemote=" + invalidateRemote +
                '}';
    }
}

