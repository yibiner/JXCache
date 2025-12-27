package dev.yibin.samples.river;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 河流服务示例应用
 * <p>
 * 功能说明：
 * 1. 提供河流信息查询服务
 * 2. 使用 @Cached 注解缓存河流数据
 * 3. 集成 JXCache Observer 功能（本地缓存可视化）
 * 4. 集成 JXCache Aggregator 功能（跨节点聚合查询）
 * 5. 支持聚合查询多个水滴服务的缓存数据
 */
@SpringBootApplication
@EnableMethodCache(basePackages = "dev.yibin.samples.river")
public class RiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiverApplication.class, args);
    }
}
