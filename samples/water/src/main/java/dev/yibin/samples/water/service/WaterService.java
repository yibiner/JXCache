package dev.yibin.samples.water.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheUpdate;
import com.alicp.jetcache.anno.Cached;
import dev.yibin.samples.water.model.Water;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 水滴服务
 * <p>
 * 演示功能：
 * 1. 使用 @Cached 注解缓存水滴数据
 * 2. 支持缓存更新和失效
 * 3. 模拟微服务多Pod场景
 */
@Service
public class WaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaterService.class);

    @Autowired
    private WaterService waterService;

    /**
     * 根据水滴ID查询水滴信息
     * <p>
     * 缓存配置（使用 default area）：
     * - 缓存区域：default（默认区域）
     * - 缓存名称：dropletCacheById
     * - 缓存键：dropletId
     * - 过期时间：30分钟
     * - 缓存类型：本地缓存（Caffeine）
     *
     * @param dropletId 水滴ID
     * @return 水滴信息
     */
    @Cached(name = "dropletCacheById", key = "#dropletId", expire = 30, cacheType = CacheType.LOCAL,
            timeUnit = TimeUnit.MINUTES, cacheNullValue = true, localExpire = 30)
    public Water getDropletById(Long dropletId) {
        LOGGER.debug("查询水滴信息，水滴ID: {}", dropletId);
        return createMockDroplet(dropletId);
    }

    /**
     * 根据水滴名称查询水滴信息
     * <p>
     * 缓存配置（使用 water area）：
     * - 缓存区域：water（自定义区域）
     * - 缓存名称：dropletCacheByName
     * - 缓存键：dropletName
     * - 过期时间：30分钟
     * - 缓存类型：本地缓存（Caffeine）
     *
     * @param dropletName 水滴名称
     * @return 水滴信息
     */
    @Cached(area = "water", name = "dropletCacheByName", key = "#dropletName", expire = 30,
            cacheType = CacheType.LOCAL, timeUnit = TimeUnit.MINUTES)
    public Water getDropletByName(String dropletName) {
        LOGGER.debug("根据水滴名称查询水滴信息: {}", dropletName);
        return createMockDropletByName(dropletName);
    }

    /**
     * 根据颜色查询水滴列表
     * <p>
     * 缓存配置（使用 water area）：
     * - 缓存区域：water（自定义区域）
     * - 缓存名称：dropletCacheByColor
     * - 缓存键：color
     * - 过期时间：30分钟
     * - 缓存类型：本地缓存（Caffeine）
     *
     * @param color 水滴颜色
     * @return 水滴列表
     */
    @Cached(area = "water", name = "dropletCacheByColor", key = "#color", cacheType = CacheType.LOCAL,
            expire = 30, timeUnit = TimeUnit.MINUTES, cacheNullValue = true, localExpire = 30)
    public List<Water> getDropletsByColor(String color) {
        LOGGER.debug("根据颜色查询水滴列表: {}", color);
        return createMockDropletsByColor(color);
    }

    /**
     * 获取水滴列表
     *
     * @param pageSize 页面大小
     * @return 水滴列表
     */
    public List<Water> getDropletList(Integer pageSize) {
        LOGGER.debug("获取水滴列表，页面大小: {}", pageSize);
        return createMockDropletList(pageSize);
    }

    /**
     * 更新水滴信息
     * <p>
     * 同时更新 default area 和 water area 的缓存
     *
     * @param droplet 水滴信息
     */
    @CacheUpdate(name = "dropletCacheById", key = "#droplet.dropletId", value = "#droplet")
    public void updateDropletById(Water droplet) {
        LOGGER.info("更新水滴信息: {}", droplet);
    }

    /**
     * 删除水滴
     * <p>
     * 同时删除 default area 和 water area 的缓存
     *
     * @param dropletId 水滴ID
     */
    @CacheInvalidate(name = "dropletCacheById", key = "#dropletId")
    public void deleteDropletById(Long dropletId) {
        LOGGER.info("删除水滴: {}", dropletId);
    }

    /**
     * 创建模拟水滴数据
     */
    private Water createMockDroplet(Long dropletId) {
        String[] colors = {"透明", "蓝色", "绿色", "红色", "黄色"};
        String[] purities = {"纯净", "微浊", "清澈", "透明", "晶莹"};

        return new Water(
                dropletId,
                "水滴" + dropletId,
                "这是水滴" + dropletId + "的详细描述",
                ThreadLocalRandom.current().nextDouble(0.1, 5.0),
                colors[dropletId.intValue() % colors.length],
                purities[dropletId.intValue() % purities.length]
        );
    }

    /**
     * 根据水滴名称创建模拟水滴数据
     */
    private Water createMockDropletByName(String dropletName) {
        Long dropletId = Math.abs(dropletName.hashCode()) % 10000L;
        return createMockDroplet(dropletId);
    }

    /**
     * 根据颜色创建模拟水滴列表
     */
    private List<Water> createMockDropletsByColor(String color) {
        List<Water> droplets = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Water droplet = new Water(
                    (long) i,
                    color + "水滴" + i,
                    "这是" + color + "水滴" + i + "的详细描述",
                    ThreadLocalRandom.current().nextDouble(0.1, 5.0),
                    color,
                    "纯净"
            );
            droplets.add(droplet);
        }
        return droplets;
    }

    /**
     * 创建模拟水滴列表
     */
    private List<Water> createMockDropletList(Integer pageSize) {
        List<Water> droplets = new ArrayList<>();
        for (int i = 1; i <= pageSize; i++) {
            droplets.add(waterService.getDropletById((long) i));
        }
        return droplets;
    }
}
