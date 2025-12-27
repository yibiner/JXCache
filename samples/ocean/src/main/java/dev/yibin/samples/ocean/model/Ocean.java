package dev.yibin.samples.ocean.model;

import java.io.Serializable;

/**
 * 海洋实体类
 */
public class Ocean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long oceanId;
    private String oceanName;
    private String description;
    private Double area; // 面积（平方公里）
    private Double depth; // 深度（米）
    private String location; // 位置
    private String temperature; // 温度范围

    public Ocean() {
    }

    public Ocean(Long oceanId, String oceanName, String description, Double area, 
                 Double depth, String location, String temperature) {
        this.oceanId = oceanId;
        this.oceanName = oceanName;
        this.description = description;
        this.area = area;
        this.depth = depth;
        this.location = location;
        this.temperature = temperature;
    }

    public Long getOceanId() {
        return oceanId;
    }

    public void setOceanId(Long oceanId) {
        this.oceanId = oceanId;
    }

    public String getOceanName() {
        return oceanName;
    }

    public void setOceanName(String oceanName) {
        this.oceanName = oceanName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Double getDepth() {
        return depth;
    }

    public void setDepth(Double depth) {
        this.depth = depth;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    @Override
    public String toString() {
        return "Ocean{" +
                "oceanId=" + oceanId +
                ", oceanName='" + oceanName + '\'' +
                ", description='" + description + '\'' +
                ", area=" + area +
                ", depth=" + depth +
                ", location='" + location + '\'' +
                ", temperature='" + temperature + '\'' +
                '}';
    }
}

