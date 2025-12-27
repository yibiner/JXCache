package dev.yibin.samples.ocean;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 海洋服务示例应用
 * <p>
 * 功能说明：
 * 1. 提供海洋信息查询服务
 * 2. 使用 @Cached 注解缓存海洋数据（BOTH 缓存类型）
 * 3. 集成 JXCache Observer 功能（本地缓存可视化）
 * 4. 集成 JXCache Aggregator 功能（跨节点聚合查询）
 * 5. 集成 Nacos 注册中心（服务发现）
 * 6. 集成 Redis（远程缓存）
 * 7. 演示真实业务场景：多Pod部署、缓存失效、广播监控等
 */
@SpringBootApplication
@EnableMethodCache(basePackages = "dev.yibin.samples.ocean")
@EnableDiscoveryClient
public class OceanApplication {

    public static void main(String[] args) {
        SpringApplication.run(OceanApplication.class, args);
    }
}

