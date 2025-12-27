package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * 缓存条目
 */
public class CacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;

    private String valuePreview;

    private String valueType;

    private long createTime;

    private long lastAccessTime;

    private long expireTime;

    public CacheEntry() {
    }

    public CacheEntry(String key, String valuePreview, String valueType) {
        this.key = key;
        this.valuePreview = valuePreview;
        this.valueType = valueType;
        long now = System.currentTimeMillis();
        this.createTime = now;
        this.lastAccessTime = now;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValuePreview() {
        return valuePreview;
    }

    public void setValuePreview(String valuePreview) {
        this.valuePreview = valuePreview;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheEntry that = (CacheEntry) o;
        return createTime == that.createTime
                && lastAccessTime == that.lastAccessTime
                && expireTime == that.expireTime
                && Objects.equals(key, that.key)
                && Objects.equals(valuePreview, that.valuePreview)
                && Objects.equals(valueType, that.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, valuePreview, valueType, createTime, lastAccessTime, expireTime);
    }

    @Override
    public String toString() {
        return "CacheEntry{" +
                "key='" + key + '\'' +
                ", valuePreview='" + valuePreview + '\'' +
                ", valueType='" + valueType + '\'' +
                ", createTime=" + createTime +
                ", lastAccessTime=" + lastAccessTime +
                ", expireTime=" + expireTime +
                '}';
    }
}
