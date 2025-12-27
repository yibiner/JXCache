package dev.yibin.samples.water.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 水滴实体类
 * <p>
 * 用于演示 JetCache Observer 功能，模拟实际业务场景中的缓存数据
 * 字段设计简洁明了，便于理解缓存查询和聚合功能
 */
public class Water implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 水滴ID，主键
     */
    private Long dropletId;

    /**
     * 水滴名称
     */
    private String dropletName;

    /**
     * 水滴描述
     */
    private String description;

    /**
     * 水滴体积（毫升）
     */
    private Double volume;

    /**
     * 水滴颜色
     */
    private String color;

    /**
     * 水滴纯度
     */
    private String purity;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public Water() {
    }

    public Water(Long dropletId, String dropletName, String description, Double volume, String color, String purity) {
        this.dropletId = dropletId;
        this.dropletName = dropletName;
        this.description = description;
        this.volume = volume;
        this.color = color;
        this.purity = purity;
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    public Water(Long dropletId, String dropletName, String description, Double volume, String color, String purity,
                 LocalDateTime createTime, LocalDateTime updateTime) {
        this.dropletId = dropletId;
        this.dropletName = dropletName;
        this.description = description;
        this.volume = volume;
        this.color = color;
        this.purity = purity;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getDropletId() {
        return dropletId;
    }

    public void setDropletId(Long dropletId) {
        this.dropletId = dropletId;
    }

    public String getDropletName() {
        return dropletName;
    }

    public void setDropletName(String dropletName) {
        this.dropletName = dropletName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPurity() {
        return purity;
    }

    public void setPurity(String purity) {
        this.purity = purity;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Water that = (Water) o;
        return Objects.equals(dropletId, that.dropletId)
                && Objects.equals(dropletName, that.dropletName)
                && Objects.equals(description, that.description)
                && Objects.equals(volume, that.volume)
                && Objects.equals(color, that.color)
                && Objects.equals(purity, that.purity)
                && Objects.equals(createTime, that.createTime)
                && Objects.equals(updateTime, that.updateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dropletId, dropletName, description, volume, color, purity, createTime, updateTime);
    }

    @Override
    public String toString() {
        return "Water{" +
                "dropletId=" + dropletId +
                ", dropletName='" + dropletName + '\'' +
                ", volume=" + volume +
                ", color='" + color + '\'' +
                ", purity='" + purity + '\'' +
                '}';
    }
}
