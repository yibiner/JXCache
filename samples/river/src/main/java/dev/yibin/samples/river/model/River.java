package dev.yibin.samples.river.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 河流实体类
 * <p>
 * 用于演示 JetCache Observer 和 Aggregator 功能，模拟实际业务场景中的缓存数据
 * 字段设计简洁明了，便于理解缓存查询和聚合功能
 */
public class River implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 河流ID，主键
     */
    private Long riverId;

    /**
     * 河流名称
     */
    private String riverName;

    /**
     * 河流描述
     */
    private String description;

    /**
     * 河流长度（公里）
     */
    private Double length;

    /**
     * 河流宽度（米）
     */
    private Double width;

    /**
     * 河流流向
     */
    private String flowDirection;

    /**
     * 水质等级
     */
    private String waterQuality;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public River() {
    }

    public River(Long riverId, String riverName, String description, Double length, Double width, String flowDirection, String waterQuality) {
        this.riverId = riverId;
        this.riverName = riverName;
        this.description = description;
        this.length = length;
        this.width = width;
        this.flowDirection = flowDirection;
        this.waterQuality = waterQuality;
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    public River(Long riverId, String riverName, String description, Double length, Double width, String flowDirection,
                 String waterQuality, LocalDateTime createTime, LocalDateTime updateTime) {
        this.riverId = riverId;
        this.riverName = riverName;
        this.description = description;
        this.length = length;
        this.width = width;
        this.flowDirection = flowDirection;
        this.waterQuality = waterQuality;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getRiverId() {
        return riverId;
    }

    public void setRiverId(Long riverId) {
        this.riverId = riverId;
    }

    public String getRiverName() {
        return riverName;
    }

    public void setRiverName(String riverName) {
        this.riverName = riverName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public String getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(String flowDirection) {
        this.flowDirection = flowDirection;
    }

    public String getWaterQuality() {
        return waterQuality;
    }

    public void setWaterQuality(String waterQuality) {
        this.waterQuality = waterQuality;
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
        River river = (River) o;
        return Objects.equals(riverId, river.riverId)
                && Objects.equals(riverName, river.riverName)
                && Objects.equals(description, river.description)
                && Objects.equals(length, river.length)
                && Objects.equals(width, river.width)
                && Objects.equals(flowDirection, river.flowDirection)
                && Objects.equals(waterQuality, river.waterQuality)
                && Objects.equals(createTime, river.createTime)
                && Objects.equals(updateTime, river.updateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(riverId, riverName, description, length, width, flowDirection, waterQuality, createTime, updateTime);
    }

    @Override
    public String toString() {
        return "River{" +
                "riverId=" + riverId +
                ", riverName='" + riverName + '\'' +
                ", length=" + length +
                ", width=" + width +
                ", flowDirection='" + flowDirection + '\'' +
                ", waterQuality='" + waterQuality + '\'' +
                '}';
    }
}
