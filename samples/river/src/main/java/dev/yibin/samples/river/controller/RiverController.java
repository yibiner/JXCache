package dev.yibin.samples.river.controller;

import dev.yibin.samples.river.model.River;
import dev.yibin.samples.river.service.RiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 河流控制器
 * 
 * 提供河流相关的 REST API 接口
 * @author Yibin
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/river")
public class RiverController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiverController.class);

    @Autowired
    private RiverService riverService;

    /**
     * 根据河流ID查询河流信息
     * 
     * @param riverId 河流ID
     * @return 河流信息
     */
    @GetMapping("/{riverId}")
    public River getRiverById(@PathVariable Long riverId) {
        LOGGER.info("查询河流信息，河流ID: {}", riverId);
        return riverService.getRiverById(riverId);
    }

    /**
     * 根据河流名称查询河流信息
     * 
     * @param riverName 河流名称
     * @return 河流信息
     */
    @GetMapping("/name/{riverName}")
    public River getRiverByName(@PathVariable String riverName) {
        LOGGER.info("根据河流名称查询河流信息: {}", riverName);
        return riverService.getRiverByName(riverName);
    }

    /**
     * 根据流向查询河流列表
     * 
     * @param flowDirection 流向
     * @return 河流列表
     */
    @GetMapping("/flow/{flowDirection}")
    public List<River> getRiversByFlowDirection(@PathVariable String flowDirection) {
        LOGGER.info("根据流向查询河流列表: {}", flowDirection);
        return riverService.getRiversByFlowDirection(flowDirection);
    }

    /**
     * 获取河流列表
     * 
     * @param pageSize 页面大小，默认10
     * @return 河流列表
     */
    @GetMapping("/list")
    public List<River> getRiverList(@RequestParam(defaultValue = "10") Integer pageSize) {
        LOGGER.info("获取河流列表，页面大小: {}", pageSize);
        return riverService.getRiverList(pageSize);
    }

    /**
     * 更新河流信息
     * 
     * @param river 河流信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public String updateRiver(@RequestBody River river) {
        LOGGER.info("更新河流信息: {}", river);
        riverService.updateRiver(river);
        return "update success";
    }

    /**
     * 删除河流
     * 
     * @param riverId 河流ID
     * @return 删除结果
     */
    @DeleteMapping("/{riverId}")
    public String deleteRiver(@PathVariable Long riverId) {
        LOGGER.info("删除河流: {}", riverId);
        riverService.deleteRiver(riverId);
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
        return "River Service is running";
    }
}
