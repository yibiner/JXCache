package dev.yibin.samples.water.controller;

import dev.yibin.samples.water.model.Water;
import dev.yibin.samples.water.service.WaterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 水滴控制器
 * <p>
 * 提供水滴相关的 REST API 接口
 * @author Yibin
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/water")
public class WaterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaterController.class);

    @Autowired
    private WaterService waterService;

    /**
     * 根据水滴ID查询水滴信息
     *
     * @param dropletId 水滴ID
     * @return 水滴信息
     */
    @GetMapping("/{dropletId}")
    public Water getDropletById(@PathVariable Long dropletId) {
        LOGGER.info("查询水滴信息，水滴ID: {}", dropletId);
        return waterService.getDropletById(dropletId);
    }

    /**
     * 根据水滴名称查询水滴信息
     *
     * @param dropletName 水滴名称
     * @return 水滴信息
     */
    @GetMapping("/name/{dropletName}")
    public Water getDropletByName(@PathVariable String dropletName) {
        LOGGER.info("根据水滴名称查询水滴信息: {}", dropletName);
        return waterService.getDropletByName(dropletName);
    }

    /**
     * 根据颜色查询水滴列表
     *
     * @param color 水滴颜色
     * @return 水滴列表
     */
    @GetMapping("/color/{color}")
    public List<Water> getDropletsByColor(@PathVariable String color) {
        LOGGER.info("根据颜色查询水滴列表: {}", color);
        return waterService.getDropletsByColor(color);
    }

    /**
     * 获取水滴列表
     *
     * @param pageSize 页面大小，默认10
     * @return 水滴列表
     */
    @GetMapping("/list")
    public List<Water> getDropletList(@RequestParam(defaultValue = "10") Integer pageSize) {
        LOGGER.info("获取水滴列表，页面大小: {}", pageSize);
        return waterService.getDropletList(pageSize);
    }

    /**
     * 更新水滴信息
     *
     * @param droplet 水滴信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public String updateDroplet(@RequestBody Water droplet) {
        LOGGER.info("更新水滴信息: {}", droplet);
        waterService.updateDropletById(droplet);
        return "update success";
    }

    /**
     * 删除水滴
     *
     * @param dropletId 水滴ID
     * @return 删除结果
     */
    @DeleteMapping("/{dropletId}")
    public String deleteDroplet(@PathVariable Long dropletId) {
        LOGGER.info("删除水滴: {}", dropletId);
        waterService.deleteDropletById(dropletId);
        return "delete success";
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public String health() {
        LOGGER.debug("健康检查请求");
        return "Water Service is running";
    }
}
