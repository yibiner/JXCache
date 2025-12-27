package dev.yibin.jxcache.aggregator.controller;

import dev.yibin.jxcache.aggregator.service.AggregateQueryService;
import dev.yibin.jxcache.common.dto.AggregateResult;
import dev.yibin.jxcache.common.dto.CacheConsistencyResult;
import dev.yibin.jxcache.common.dto.InvalidateRequest;
import dev.yibin.jxcache.common.dto.InvalidateResult;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.common.dto.ServiceInstance;
import dev.yibin.jxcache.registry.spi.RegistryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聚合查询控制器
 * 提供聚合查询相关的 REST API
 * @author Yibin
 * @since 1.0.0
 */
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/jxc/aggregate")
public class AggregateController {

    private static final Logger logger = LoggerFactory.getLogger(AggregateController.class);

    @Autowired
    private AggregateQueryService aggregateQueryService;

    @Autowired
    private RegistryClientFactory registryClientFactory;

    /**
     * 获取服务节点列表
     */
    @GetMapping("/nodes")
    public List<Map<String, Object>> listNodes(@RequestParam String serviceName) {
        try {
            logger.debug("[Aggregator] Listing nodes for service: {}", serviceName);
            if (registryClientFactory == null) {
                logger.warn("[Aggregator] RegistryClientFactory is null");
                return new ArrayList<>();
            }

            dev.yibin.jxcache.registry.spi.RegistryClient client =
                    registryClientFactory.getRegistryClient(serviceName);

            if (client == null) {
                logger.warn("[Aggregator] No registry client found for service: {}", serviceName);
                return new ArrayList<>();
            }

            List<ServiceInstance> instances = client.listInstances(serviceName);
            logger.debug("[Aggregator] Found {} instances for service: {}", instances.size(), serviceName);

            List<Map<String, Object>> result = instances.stream()
                    .map(instance -> {
                        Map<String, Object> node = new HashMap<>();
                        node.put("nodeId", instance.getNodeId());
                        node.put("host", instance.getHost());
                        node.put("port", instance.getPort());
                        node.put("healthy", instance.isHealthy());
                        if (instance.getMetadata() != null) {
                            node.put("metadata", instance.getMetadata());
                        }
                        return node;
                    })
                    .collect(Collectors.toList());

            return result;
        } catch (Exception e) {
            logger.error("[Aggregator] Failed to list nodes for service: {}", serviceName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 聚合查询多个节点的本地缓存数据（支持 GET 和 POST）
     * <p>
     * GET 方式：适用于简单查询，参数通过 query parameters 传递
     * POST 方式：适用于复杂查询，参数通过 request body 传递（支持分页、分片等复杂条件）
     * <p>
     * 为了符合 RESTful 规范，优先使用 GET 方式；复杂查询场景可使用 POST 方式
     */
    @GetMapping("/query")
    public AggregateResult queryGet(@RequestParam String serviceName,
                                    @RequestParam(required = false) List<String> targets,
                                    @RequestParam String area,
                                    @RequestParam String cacheName,
                                    @RequestParam(required = false) String keyPrefix,
                                    @RequestParam(required = false, defaultValue = "1") int pageNo,
                                    @RequestParam(required = false, defaultValue = "20") int pageSize,
                                    @RequestParam(required = false, defaultValue = "0") int shard,
                                    @RequestParam(required = false, defaultValue = "1") int totalShards) {
        QueryRequest request = new QueryRequest();
        request.setArea(area);
        request.setCacheName(cacheName);
        request.setKeyPrefix(keyPrefix);
        request.setPageRequest(new dev.yibin.jxcache.common.dto.PageRequest(pageNo, pageSize));
        request.setShard(shard);
        request.setTotalShards(totalShards);
        return aggregateQueryService.aggregateQuery(serviceName, request, targets);
    }

    /**
     * 聚合查询多个节点的本地缓存数据（POST 方式，支持复杂查询条件）
     * <p>
     * 当查询条件复杂（如需要嵌套的分页对象、多个分片参数等）时，使用 POST 方式
     */
    @PostMapping("/query")
    public AggregateResult queryPost(@RequestParam String serviceName,
                                     @RequestParam(required = false) List<String> targets,
                                     @RequestBody QueryRequest request) {
        return aggregateQueryService.aggregateQuery(serviceName, request, targets);
    }

    /**
     * 聚合查询多个节点的单个缓存条目
     *
     * @param serviceName 服务名称
     * @param area        缓存区域
     * @param cacheName   缓存名称
     * @param key         缓存键
     * @param targets     目标节点列表，为空则查询所有节点
     * @return 缓存条目详情列表（每个节点一个）
     */
    @GetMapping("/entry")
    public List<LocalCacheEntryDetail> getEntry(@RequestParam String serviceName,
                                                @RequestParam String area,
                                                @RequestParam("name") String cacheName,
                                                @RequestParam String key,
                                                @RequestParam(required = false) List<String> targets) {
        logger.debug("[Aggregator] Aggregating entry query, service: {}, area: {}, cacheName: {}, key: {}",
                serviceName, area, cacheName, key);
        return aggregateQueryService.aggregateEntry(serviceName, area, cacheName, key, targets);
    }

    /**
     * 检查多个节点的单个缓存条目是否一致
     * <p>
     * 该接口用于检查各个服务节点对应相同 key 的本地缓存数据是否一致。
     * 返回结果包含一致性状态、各节点的缓存值详情以及不一致的原因。
     *
     * @param serviceName 服务名称
     * @param area        缓存区域
     * @param cacheName   缓存名称
     * @param key         缓存键
     * @param targets     目标节点列表，为空则查询所有节点
     * @return 缓存一致性检查结果
     */
    @GetMapping("/entry/consistency")
    public CacheConsistencyResult checkConsistency(@RequestParam String serviceName,
                                                   @RequestParam String area,
                                                   @RequestParam("name") String cacheName,
                                                   @RequestParam String key,
                                                   @RequestParam(required = false) List<String> targets) {
        logger.debug("[Aggregator] Checking cache consistency, service: {}, area: {}, cacheName: {}, key: {}",
                serviceName, area, cacheName, key);
        return aggregateQueryService.checkConsistency(serviceName, area, cacheName, key, targets);
    }

    /**
     * 聚合失效多个节点的本地缓存（DELETE 方式）
     * <p>
     * 支持清除所有节点的本地缓存，也支持清除指定的某节点缓存。
     * 默认情况下只清除本地缓存，通过 invalidateRemote 参数确定是否清除对应的 Redis 缓存。
     *
     * @param serviceName      服务名称
     * @param area             缓存区域
     * @param cacheName        缓存名称
     * @param key              缓存键（可选，为空则清除整个缓存）
     * @param targets          目标节点列表，为空则清除所有节点
     * @param invalidateRemote 是否同时清除 Redis 缓存，默认 false
     * @return 失效结果列表（每个节点一个）
     */
    @DeleteMapping("/invalidate")
    public List<InvalidateResult> aggregateInvalidate(@RequestParam String serviceName,
                                                      @RequestParam String area,
                                                      @RequestParam("name") String cacheName,
                                                      @RequestParam(required = false) String key,
                                                      @RequestParam(required = false) List<String> targets,
                                                      @RequestParam(required = false, defaultValue = "false") boolean invalidateRemote) {
        logger.debug("[Aggregator] Aggregating invalidate, service: {}, area: {}, cacheName: {}, key: {}, targets: {}",
                serviceName, area, cacheName, key, targets);
        InvalidateRequest request = new InvalidateRequest(area, cacheName, key);
        request.setInvalidateRemote(invalidateRemote);
        return aggregateQueryService.aggregateInvalidate(serviceName, request, targets);
    }

    /**
     * 聚合失效多个节点的本地缓存（POST 方式，支持复杂请求体）
     *
     * @param serviceName 服务名称
     * @param targets     目标节点列表，为空则清除所有节点
     * @param request     失效请求
     * @return 失效结果列表（每个节点一个）
     */
    @PostMapping("/invalidate")
    public List<InvalidateResult> aggregateInvalidatePost(@RequestParam String serviceName,
                                                          @RequestParam(required = false) List<String> targets,
                                                          @RequestBody InvalidateRequest request) {
        logger.debug("[Aggregator] Aggregating invalidate (POST), service: {}, area: {}, cacheName: {}, key: {}, targets: {}",
                serviceName, request != null ? request.getArea() : null, 
                request != null ? request.getCacheName() : null,
                request != null ? request.getKey() : null, targets);
        return aggregateQueryService.aggregateInvalidate(serviceName, request, targets);
    }
}
