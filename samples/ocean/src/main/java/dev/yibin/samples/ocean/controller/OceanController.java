package dev.yibin.samples.ocean.controller;

import dev.yibin.samples.ocean.model.Ocean;
import dev.yibin.samples.ocean.service.OceanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 海洋服务控制器
 * @author Yibin
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ocean")
public class OceanController {

    @Autowired
    private OceanService oceanService;

    /**
     * 根据ID查询海洋信息
     */
    @GetMapping("/{oceanId}")
    public Ocean getOceanById(@PathVariable Long oceanId) {
        return oceanService.getOceanById(oceanId);
    }

    /**
     * 根据名称查询海洋信息
     */
    @GetMapping("/name/{oceanName}")
    public Ocean getOceanByName(@PathVariable String oceanName) {
        return oceanService.getOceanByName(oceanName);
    }

    /**
     * 根据位置查询海洋列表
     */
    @GetMapping("/location/{location}")
    public List<Ocean> getOceansByLocation(@PathVariable String location) {
        return oceanService.getOceansByLocation(location);
    }

    /**
     * 获取海洋列表
     */
    @GetMapping("/list")
    public List<Ocean> getOceanList(@RequestParam(defaultValue = "10") Integer pageSize) {
        return oceanService.getOceanList(pageSize);
    }

    /**
     * 更新海洋信息
     */
    @PutMapping("/update")
    public void updateOcean(@RequestBody Ocean ocean) {
        oceanService.updateOcean(ocean);
    }

    /**
     * 删除海洋
     */
    @DeleteMapping("/{oceanId}")
    public void deleteOcean(@PathVariable Long oceanId) {
        oceanService.deleteOcean(oceanId);
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public String health() {
        return "Ocean Service is running";
    }
}

