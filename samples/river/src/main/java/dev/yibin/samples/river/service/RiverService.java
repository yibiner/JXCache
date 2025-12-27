package dev.yibin.samples.river.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheUpdate;
import com.alicp.jetcache.anno.Cached;
import dev.yibin.samples.river.model.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 河流服务
 * <p>
 * 演示功能：
 * 1. 使用 @Cached 注解缓存河流数据
 * 2. 支持缓存更新和失效
 * 3. 聚水滴成河的业务逻辑
 */
@Service
public class RiverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiverService.class);

    /**
     * 根据河流ID查询河流信息
     * <p>
     * 缓存配置（使用 default area）：
     * - 缓存区域：default（默认区域）
     * - 缓存名称：riverCacheById
     * - 缓存键：riverId
     * - 过期时间：30分钟
     * - 缓存类型：本地缓存（Caffeine）
     *
     * @param riverId 河流ID
     * @return 河流信息
     */
    @Cached(name = "riverCacheById", key = "#riverId", expire = 30, cacheType = CacheType.LOCAL,
            timeUnit = TimeUnit.MINUTES)
    public River getRiverById(Long riverId) {
        LOGGER.debug("查询河流信息，河流ID: {}", riverId);
        River river = createMockRiver(riverId);
        LOGGER.debug("河流信息查询完成: {}", river);
        return river;
    }

    /**
     * 根据河流名称查询河流信息
     * <p>
     * 缓存配置（使用 river area）：
     * - 缓存区域：river（自定义区域）
     * - 缓存名称：riverCacheByName
     * - 缓存键：riverName
     * - 过期时间：30分钟
     * - 缓存类型：本地缓存（Caffeine）
     *
     * @param riverName 河流名称
     * @return 河流信息
     */
    @Cached(area = "river", name = "riverCacheByName", key = "#riverName", expire = 30,
            cacheType = CacheType.LOCAL, timeUnit = TimeUnit.MINUTES)
    public River getRiverByName(String riverName) {
        LOGGER.debug("根据河流名称查询河流信息: {}", riverName);
        River river = createMockRiverByName(riverName);
        LOGGER.debug("河流信息查询完成: {}", river);
        return river;
    }

    /**
     * 根据流向查询河流列表
     * <p>
     * 缓存配置（使用 river area）：
     * - 缓存区域：river（自定义区域）
     * - 缓存名称：riverCacheByFlowDirection
     * - 缓存键：flowDirection
     * - 过期时间：30分钟
     * - 缓存类型：本地缓存（Caffeine）
     *
     * @param flowDirection 流向
     * @return 河流列表
     */
    @Cached(area = "river", name = "riverCacheByFlowDirection", key = "#flowDirection", expire = 30,
            cacheType = CacheType.LOCAL, timeUnit = TimeUnit.MINUTES)
    public List<River> getRiversByFlowDirection(String flowDirection) {
        LOGGER.debug("根据流向查询河流列表: {}", flowDirection);
        List<River> rivers = createMockRiversByFlowDirection(flowDirection);
        LOGGER.debug("河流列表查询完成，数量: {}", rivers.size());
        return rivers;
    }

    /**
     * 获取河流列表
     * <p>
     * 缓存配置（使用 default area）：
     * - 缓存区域：default（默认区域）
     * - 缓存名称：riverCacheList
     * - 缓存键：pageSize
     * - 过期时间：30分钟
     * - 缓存类型：本地缓存（Caffeine）
     *
     * @param pageSize 页面大小
     * @return 河流列表
     */
    @Cached(name = "riverCacheList", key = "#pageSize", expire = 30, cacheType = CacheType.LOCAL,
            timeUnit = TimeUnit.MINUTES)
    public List<River> getRiverList(Integer pageSize) {
        LOGGER.debug("获取河流列表，页面大小: {}", pageSize);
        List<River> rivers = createMockRiverList(pageSize);
        LOGGER.debug("河流列表查询完成，数量: {}", rivers.size());
        return rivers;
    }

    /**
     * 更新河流信息
     * <p>
     * 同时更新 default area 和 river area 的缓存
     *
     * @param river 河流信息
     */
    @CacheUpdate(name = "riverCacheById", key = "#river.riverId", value = "#river")
    public void updateRiver(River river) {
        LOGGER.info("更新河流信息: {}", river);
    }

    /**
     * 删除河流
     * <p>
     * 同时删除 default area 和 river area 的缓存
     *
     * @param riverId 河流ID
     */
    @CacheInvalidate(name = "riverCacheById", key = "#riverId")
    public void deleteRiver(Long riverId) {
        LOGGER.info("删除河流: {}", riverId);
    }

    /**
     * 创建模拟河流数据
     */
    private River createMockRiver(Long riverId) {
        String[] flowDirections = {"东", "西", "南", "北", "东南", "西南", "东北", "西北"};
        String[] waterQualities = {"清澈", "微浊", "透明", "纯净", "甘甜"};

        return new River(
                riverId,
                "河流" + riverId,
                "这是河流" + riverId + "的详细描述",
                ThreadLocalRandom.current().nextDouble(10.0, 1000.0),
                ThreadLocalRandom.current().nextDouble(1.0, 50.0),
                flowDirections[riverId.intValue() % flowDirections.length],
                waterQualities[riverId.intValue() % waterQualities.length]
        );
    }

    /**
     * 根据河流名称创建模拟河流数据
     */
    private River createMockRiverByName(String riverName) {
        Long riverId = Math.abs(riverName.hashCode()) % 10000L;
        return createMockRiver(riverId);
    }

    /**
     * 根据流向创建模拟河流列表
     */
    private List<River> createMockRiversByFlowDirection(String flowDirection) {
        List<River> rivers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            River river = new River(
                    (long) i,
                    flowDirection + "向河流" + i,
                    "这是" + flowDirection + "向河流" + i + "的详细描述",
                    ThreadLocalRandom.current().nextDouble(10.0, 1000.0),
                    ThreadLocalRandom.current().nextDouble(1.0, 50.0),
                    flowDirection,
                    "清澈"
            );
            rivers.add(river);
        }
        return rivers;
    }

    /**
     * 创建模拟河流列表
     */
    private List<River> createMockRiverList(Integer pageSize) {
        List<River> rivers = new ArrayList<>();
        for (int i = 1; i <= pageSize; i++) {
            rivers.add(createMockRiver((long) i));
        }
        return rivers;
    }
}
