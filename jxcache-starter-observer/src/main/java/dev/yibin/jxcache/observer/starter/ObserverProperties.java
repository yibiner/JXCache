package dev.yibin.jxcache.observer.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Observer 配置属性
 * @author Yibin
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "jxc.observer")
public class ObserverProperties {
    
    /**
     * 是否启用 Observer
     */
    private boolean enabled = true;
    
    /**
     * 最大分页大小
     */
    private int maxPageSize = 200;
    
    /**
     * 最大值预览长度
     */
    private int maxValuePreview = 200;
    
    /**
     * 总分片数
     */
    private int totalShards = 8;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getMaxPageSize() {
        return maxPageSize;
    }
    
    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
    
    public int getMaxValuePreview() {
        return maxValuePreview;
    }
    
    public void setMaxValuePreview(int maxValuePreview) {
        this.maxValuePreview = maxValuePreview;
    }
    
    public int getTotalShards() {
        return totalShards;
    }
    
    public void setTotalShards(int totalShards) {
        this.totalShards = totalShards;
    }
}
