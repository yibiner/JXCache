package dev.yibin.jxcache.observer.controller;

import dev.yibin.jxcache.common.dto.*;
import dev.yibin.jxcache.observer.exception.CacheNotFoundException;
import dev.yibin.jxcache.observer.service.LocalCacheService;
import dev.yibin.jxcache.observer.support.JetCacheAreaRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 本地缓存 API 控制器
 * 提供对外查询本地缓存的 REST API
 * <p>
 * 支持多种本地缓存类型（CaffeineCache、LinkedHashMapCache 等）
 * 通过 LocalCacheService 自动选择合适的内省器
 * @author Yibin
 * @since 1.0.0
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/jxc/observer")
public class ObserverController {

    @Autowired
    private LocalCacheService localCacheService;

    @Autowired
    private JetCacheAreaRegistry jetCacheAreaRegistry;

    /**
     * 获取所有缓存区域
     *
     * @return 缓存区域列表
     */
    @GetMapping("/areas")
    public List<String> getAreas() {
        if (jetCacheAreaRegistry == null) {
            return new ArrayList<>();
        }
        Set<String> areas = jetCacheAreaRegistry.listAreas();
        return new ArrayList<>(areas);
    }

    /**
     * 刷新当前服务节点的 JetCache areas 配置
 */
    @PostMapping("/areas/recompute")
    public void recompute() {
        jetCacheAreaRegistry.recompute();
    }

    /**
     * 查询缓存数据（支持 GET 和 POST）
     * <p>
     * GET 方式：适用于简单查询，参数通过 query parameters 传递
     * POST 方式：适用于复杂查询，参数通过 request body 传递（支持分页、分片等复杂条件）
     * <p>
     * 为了符合 RESTful 规范，优先使用 GET 方式；复杂查询场景可使用 POST 方式
     */
    @GetMapping("/query")
    public LocalCacheSnapshot queryGet(@RequestParam String area,
                                       @RequestParam String cacheName,
                                       @RequestParam(required = false, defaultValue = "L0") String level,
                                       @RequestParam(required = false) String keyPrefix,
                                       @RequestParam(required = false, defaultValue = "1") int pageNo,
                                       @RequestParam(required = false, defaultValue = "20") int pageSize,
                                       @RequestParam(required = false, defaultValue = "0") int shard,
                                       @RequestParam(required = false, defaultValue = "1") int totalShards) {
        if (isBlank(area) || isBlank(cacheName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "area and cacheName are required");
        }
        QueryRequest request = new QueryRequest();
        request.setArea(area);
        request.setCacheName(cacheName);
        request.setLevel(level);
        request.setKeyPrefix(keyPrefix);
        request.setPageRequest(new dev.yibin.jxcache.common.dto.PageRequest(pageNo, pageSize));
        request.setShard(shard);
        request.setTotalShards(totalShards);
        return localCacheService.scan(request);
    }

    /**
     * 查询缓存数据（POST 方式，支持复杂查询条件）
     * <p>
     * 当查询条件复杂（如需要嵌套的分页对象、多个分片参数等）时，使用 POST 方式
     */
    @PostMapping("/query")
    public LocalCacheSnapshot queryPost(@RequestBody QueryRequest request) {
        return localCacheService.scan(request);
    }

    /**
     * 获取单个缓存键的完整值
     */
    @GetMapping("/entry")
    public LocalCacheEntryDetail getEntry(@RequestParam String area,
                                          @RequestParam("name") String cacheName,
                                          @RequestParam String key,
                                          @RequestParam(required = false, defaultValue = "AUTO") String level) {
        if (isBlank(area) || isBlank(cacheName) || isBlank(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "area, name and key are required");
        }
        try {
            return localCacheService.findEntry(area, cacheName, key, level)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            String.format("Cache entry not found: area=%s, name=%s, key=%s, level=%s", area, cacheName, key, level)));
        } catch (CacheNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 失效本地缓存
     * <p>
     * 默认情况下只清除本地缓存，通过 invalidateRemote 参数确定是否清除对应的 Redis 缓存
     *
     * @param area             缓存区域
     * @param cacheName        缓存名称
     * @param key              缓存键（可选，为空则清除整个缓存）
     * @param invalidateRemote 是否同时清除 Redis 缓存，默认 false
     * @return 失效结果
     */
    @DeleteMapping("/invalidate")
    public InvalidateResult invalidate(@RequestParam String area,
                                       @RequestParam("name") String cacheName,
                                       @RequestParam(required = false) String key,
                                       @RequestParam(required = false, defaultValue = "false") boolean invalidateRemote) {
        InvalidateRequest request = new InvalidateRequest(area, cacheName, key);
        request.setInvalidateRemote(invalidateRemote);
        return localCacheService.invalidate(request);
    }

    /**
     * 失效本地缓存（POST 方式，支持复杂请求体）
     *
     * @param request 失效请求
     * @return 失效结果
     */
    @PostMapping("/invalidate")
    public InvalidateResult invalidatePost(@RequestBody InvalidateRequest request) {
        return localCacheService.invalidate(request);
    }
}
