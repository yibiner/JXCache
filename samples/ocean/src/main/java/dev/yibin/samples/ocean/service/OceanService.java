package dev.yibin.samples.ocean.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheUpdate;
import com.alicp.jetcache.anno.Cached;
import dev.yibin.samples.ocean.model.Ocean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 海洋服务
 * <p>
 * 演示功能：
 * 1. 使用 @Cached 注解缓存海洋数据（BOTH 缓存类型）
 * 2. 支持缓存更新和失效
 * 3. 演示多Pod场景下的缓存一致性
 */
@Service
public class OceanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OceanService.class);

    /**
     * 根据海洋ID查询海洋信息
     * <p>
     * 缓存配置（使用 default area）：
     * - 缓存区域：default（默认区域）
     * - 缓存名称：oceanCacheById
     * - 缓存键：oceanId
     * - 过期时间：30分钟
     * - 缓存类型：BOTH（本地缓存 + 远程缓存）
     *
     * @param oceanId 海洋ID
     * @return 海洋信息
     */
    @Cached(name = "oceanCacheById", key = "#oceanId", expire = 30, cacheType = CacheType.BOTH,
            timeUnit = TimeUnit.MINUTES, localExpire = 30)
    public Ocean getOceanById(Long oceanId) {
        LOGGER.debug("查询海洋信息，海洋ID: {}", oceanId);
        Ocean ocean = createMockOcean(oceanId);
        LOGGER.debug("海洋信息查询完成: {}", ocean);
        return ocean;
    }

    /**
     * 根据海洋名称查询海洋信息
     * <p>
     * 缓存配置（使用 ocean area）：
     * - 缓存区域：ocean（自定义区域）
     * - 缓存名称：oceanCacheByName
     * - 缓存键：oceanName
     * - 过期时间：30分钟
     * - 缓存类型：BOTH（本地缓存 + 远程缓存）
     *
     * @param oceanName 海洋名称
     * @return 海洋信息
     */
    @Cached(area = "ocean", name = "oceanCacheByName", key = "#oceanName", expire = 30,
            cacheType = CacheType.BOTH, timeUnit = TimeUnit.MINUTES, localExpire = 30)
    public Ocean getOceanByName(String oceanName) {
        LOGGER.debug("根据海洋名称查询海洋信息: {}", oceanName);
        Ocean ocean = createMockOceanByName(oceanName);
        LOGGER.debug("海洋信息查询完成: {}", ocean);
        return ocean;
    }

    /**
     * 根据位置查询海洋列表
     * <p>
     * 缓存配置（使用 ocean area）：
     * - 缓存区域：ocean（自定义区域）
     * - 缓存名称：oceanCacheByLocation
     * - 缓存键：location
     * - 过期时间：30分钟
     * - 缓存类型：BOTH（本地缓存 + 远程缓存）
     *
     * @param location 位置
     * @return 海洋列表
     */
    @Cached(area = "ocean", name = "oceanCacheByLocation", key = "#location", expire = 30,
            cacheType = CacheType.BOTH, timeUnit = TimeUnit.MINUTES, localExpire = 30)
    public List<Ocean> getOceansByLocation(String location) {
        LOGGER.debug("根据位置查询海洋列表: {}", location);
        List<Ocean> oceans = createMockOceansByLocation(location);
        LOGGER.debug("海洋列表查询完成，数量: {}", oceans.size());
        return oceans;
    }

    /**
     * 获取海洋列表
     * <p>
     * 缓存配置（使用 default area）：
     * - 缓存区域：default（默认区域）
     * - 缓存名称：oceanCacheList
     * - 缓存键：pageSize
     * - 过期时间：30分钟
     * - 缓存类型：BOTH（本地缓存 + 远程缓存）
     *
     * @param pageSize 页面大小
     * @return 海洋列表
     */
    @Cached(name = "oceanCacheList", key = "#pageSize", expire = 30, cacheType = CacheType.BOTH,
            timeUnit = TimeUnit.MINUTES, localExpire = 30)
    public List<Ocean> getOceanList(Integer pageSize) {
        LOGGER.debug("获取海洋列表，页面大小: {}", pageSize);
        List<Ocean> oceans = createMockOceanList(pageSize);
        LOGGER.debug("海洋列表查询完成，数量: {}", oceans.size());
        return oceans;
    }

    /**
     * 更新海洋信息
     * <p>
     * 同时更新 default area 和 ocean area 的缓存
     *
     * @param ocean 海洋信息
     */
    @CacheUpdate(name = "oceanCacheById", key = "#ocean.oceanId", value = "#ocean")
    public void updateOcean(Ocean ocean) {
        LOGGER.info("更新海洋信息: {}", ocean);
    }

    /**
     * 删除海洋
     * <p>
     * 同时删除 default area 和 ocean area 的缓存
     *
     * @param oceanId 海洋ID
     */
    @CacheInvalidate(name = "oceanCacheById", key = "#oceanId")
    public void deleteOcean(Long oceanId) {
        LOGGER.info("删除海洋: {}", oceanId);
    }

    /**
     * 创建模拟海洋数据
     */
    private Ocean createMockOcean(Long oceanId) {
        String[] locations = {"太平洋", "大西洋", "印度洋", "北冰洋", "南冰洋"};
        String[] temperatures = {"0-10°C", "10-20°C", "20-30°C", "15-25°C", "5-15°C"};

        return new Ocean(
                oceanId,
                "海洋" + oceanId,
                "这是海洋" + oceanId + "的详细描述",
                ThreadLocalRandom.current().nextDouble(1000.0, 100000.0),
                ThreadLocalRandom.current().nextDouble(100.0, 10000.0),
                locations[oceanId.intValue() % locations.length],
                temperatures[oceanId.intValue() % temperatures.length]
        );
    }

    /**
     * 根据海洋名称创建模拟海洋数据
     */
    private Ocean createMockOceanByName(String oceanName) {
        Long oceanId = Math.abs(oceanName.hashCode()) % 10000L;
        return createMockOcean(oceanId);
    }

    /**
     * 根据位置创建模拟海洋列表
     */
    private List<Ocean> createMockOceansByLocation(String location) {
        List<Ocean> oceans = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Ocean ocean = new Ocean(
                    (long) i,
                    location + "海洋" + i,
                    "这是" + location + "海洋" + i + "的详细描述",
                    ThreadLocalRandom.current().nextDouble(1000.0, 100000.0),
                    ThreadLocalRandom.current().nextDouble(100.0, 10000.0),
                    location,
                    "10-20°C"
            );
            oceans.add(ocean);
        }
        return oceans;
    }

    /**
     * 创建模拟海洋列表
     */
    private List<Ocean> createMockOceanList(Integer pageSize) {
        List<Ocean> oceans = new ArrayList<>();
        for (int i = 1; i <= pageSize; i++) {
            oceans.add(createMockOcean((long) i));
        }
        return oceans;
    }
}

