package dev.yibin.samples.water;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 水滴服务示例应用
 * 
 * 功能说明：
 * 1. 提供水滴信息查询服务
 * 2. 使用 @Cached 注解缓存水滴数据
 * 3. 集成 JXCache Observer 功能
 * 4. 支持本地缓存可视化查询
 * 5. 模拟微服务多Pod场景
 */
@SpringBootApplication
@EnableMethodCache(basePackages = "dev.yibin.samples.water")
public class WaterApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaterApplication.class, args);
    }
}
