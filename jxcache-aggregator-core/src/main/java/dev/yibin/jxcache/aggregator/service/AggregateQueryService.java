package dev.yibin.jxcache.aggregator.service;

import dev.yibin.jxcache.common.dto.AggregateResult;
import dev.yibin.jxcache.common.dto.CacheConsistencyResult;
import dev.yibin.jxcache.common.dto.InvalidateRequest;
import dev.yibin.jxcache.common.dto.InvalidateResult;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.LocalCacheSnapshot;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.common.dto.ServiceInstance;
import dev.yibin.jxcache.registry.spi.RegistryClient;
import dev.yibin.jxcache.registry.spi.RegistryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 聚合查询服务
 * 负责从多个服务节点聚合查询本地缓存数据
 */
@Service
public class AggregateQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(AggregateQueryService.class);
    
    @Autowired
    private RegistryClientFactory registryClientFactory;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${jxc.aggregator.perNodeTimeoutMs:2000}")
    private int perNodeTimeoutMs;
    
    @Value("${jxc.aggregator.totalTimeoutMs:4000}")
    private int totalTimeoutMs;
    
    @Value("${jxc.aggregator.maxConcurrency:16}")
    private int maxConcurrency;
    
    @Value("${jxc.aggregator.observerScanPath:/api/jxc/observer/query}")
    private String observerScanPath;
    
    @Value("${jxc.aggregator.observerEntryPath:/api/jxc/observer/entry}")
    private String observerEntryPath;
    
    @Value("${jxc.aggregator.observerInvalidatePath:/api/jxc/observer/invalidate}")
    private String observerInvalidatePath;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "jxc-aggregator-timeout");
                t.setDaemon(true);
                return t;
            });
    
    private ExecutorService executorService;
    
    /**
     * 初始化 ExecutorService
     * 在 Spring 注入完所有字段后初始化，确保 maxConcurrency 有值
     */
    @PostConstruct
    public void init() {
        if (maxConcurrency <= 0) {
            maxConcurrency = 16; // 默认值
            logger.warn("[Aggregator] Invalid maxConcurrency value, using default: 16");
        }
        executorService = Executors.newFixedThreadPool(maxConcurrency, r -> {
            Thread t = new Thread(r, "jxc-aggregator-worker");
            t.setDaemon(true);
            return t;
        });
        logger.debug("[Aggregator] Initialized ExecutorService with maxConcurrency: {}", maxConcurrency);
    }
    
    /**
     * 清理资源
     */
    @PreDestroy
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        logger.debug("[Aggregator] ExecutorService and Scheduler shutdown completed");
    }
    
    /**
     * 聚合查询多个节点的本地缓存数据
     * 
     * @param serviceName 服务名称
     * @param request 查询请求
     * @param targetNodes 目标节点列表，为空则查询所有节点
     * @return 聚合查询结果
     */
    public AggregateResult aggregateQuery(String serviceName, QueryRequest request, List<String> targetNodes) {
        if (serviceName == null || serviceName.isEmpty()) {
            logger.warn("[Aggregator] Service name is null or empty");
            return createEmptyResult();
        }
        
        if (request == null) {
            logger.warn("[Aggregator] Query request is null, service: {}", serviceName);
            return createEmptyResult();
        }
        
        if (registryClientFactory == null || restTemplate == null) {
            logger.error("[Aggregator] Required dependencies are not available");
            return createErrorResult(new IllegalStateException("Dependencies not available"));
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            RegistryClient client = registryClientFactory.getRegistryClient(serviceName);
            if (client == null) {
                logger.warn("[Aggregator] No registry client found for service: {}", serviceName);
                return createEmptyResult();
            }
            
            List<ServiceInstance> instances = client.listInstances(serviceName);
            
            if (targetNodes != null && !targetNodes.isEmpty()) {
                instances = instances.stream()
                        .filter(instance -> instance != null && instance.getNodeId() != null 
                                && targetNodes.contains(instance.getNodeId()))
                        .collect(Collectors.toList());
            }
            
            if (instances.isEmpty()) {
                logger.warn("[Aggregator] No instances found for service: {}", serviceName);
                return createEmptyResult();
            }
            
            List<CompletableFuture<LocalCacheSnapshot>> futures = instances.stream()
                    .map(instance -> queryNodeAsync(instance, request))
                    .collect(Collectors.toList());
            
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            
            try {
                allFutures.get(totalTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.warn("[Aggregator] Some queries timed out or failed, service: {}", serviceName, e);
            }
            
            List<LocalCacheSnapshot> results = new ArrayList<>();
            List<String> failedNodes = new ArrayList<>();
            
            // 已完成的任务直接获取，未完成的任务再等待
            for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<LocalCacheSnapshot> future = futures.get(i);
                ServiceInstance instance = instances.get(i);
                
                try {
                    LocalCacheSnapshot snapshot = null;
                    // 如果已完成，直接获取结果，避免不必要的等待
                    if (future.isDone()) {
                        try {
                            snapshot = future.getNow(null);
                        } catch (Exception e) {
                            // getNow 不会抛出异常，但为了安全起见保留 try-catch
                            logger.debug("[Aggregator] Future is done but getNow failed for node: {}", 
                                    instance.getNodeId(), e);
                        }
                    } else {
                        // 未完成的任务，使用短超时获取（避免长时间等待）
                        try {
                            snapshot = future.get(50, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            // 超时或异常，标记为失败
                            logger.debug("[Aggregator] Failed to get result from node: {}, service: {}", 
                                    instance.getNodeId(), serviceName, e);
                        }
                    }
                    
                    if (snapshot != null) {
                        results.add(snapshot);
                    } else {
                        String nodeId = instance.getNodeId() != null ? instance.getNodeId() : "unknown";
                        failedNodes.add(nodeId);
                    }
                } catch (Exception e) {
                    logger.warn("[Aggregator] Failed to get result from node: {}, service: {}", 
                            instance.getNodeId() != null ? instance.getNodeId() : "unknown", serviceName, e);
                    String nodeId = instance.getNodeId() != null ? instance.getNodeId() : "unknown";
                    failedNodes.add(nodeId);
                }
            }
            
            AggregateResult result = new AggregateResult();
            result.setResults(results);
            result.setFailedNodes(failedNodes);
            result.setPartial(!failedNodes.isEmpty());
            result.setTotalTimeMs(System.currentTimeMillis() - startTime);
            
            logger.debug("[Aggregator] Query completed, service: {}, results: {}, failed: {}, time: {}ms", 
                    serviceName, results.size(), failedNodes.size(), result.getTotalTimeMs());
            
            return result;
            
        } catch (Exception e) {
            logger.error("[Aggregator] Query failed, service: {}", serviceName, e);
            return createErrorResult(e);
        }
    }
    
    /**
     * 异步查询单个节点
     */
    private CompletableFuture<LocalCacheSnapshot> queryNodeAsync(ServiceInstance instance, QueryRequest request) {
        if (instance == null || request == null) {
            return CompletableFuture.completedFuture(null);
        }

        return Futures8.withTimeout(
                CompletableFuture.supplyAsync(() -> {
                    try {
                        String url = instance.getUrl() + observerScanPath;
                        LocalCacheSnapshot snapshot =
                                restTemplate.postForObject(url, request, LocalCacheSnapshot.class);
                        if (snapshot != null && instance.getNodeId() != null) {
                            snapshot.setNodeId(instance.getNodeId());
                        }
                        return snapshot;
                    } catch (Exception e) {
                        String nodeId = instance.getNodeId() != null ? instance.getNodeId() : "unknown";
                        logger.warn("[Aggregator] Failed to query node: {}, url: {}", nodeId, 
                                instance.getUrl() + observerScanPath, e);
                        return null;
                    }
                }, executorService),
                perNodeTimeoutMs, TimeUnit.MILLISECONDS, scheduler
        );
    }
    
    private AggregateResult createEmptyResult() {
        AggregateResult result = new AggregateResult();
        result.setResults(new ArrayList<>());
        result.setFailedNodes(new ArrayList<>());
        result.setPartial(false);
        return result;
    }
    
    /**
     * 聚合查询多个节点的单个缓存条目
     * 
     * @param serviceName 服务名称
     * @param area 缓存区域
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param targetNodes 目标节点列表，为空则查询所有节点
     * @return 缓存条目详情列表（每个节点一个）
     */
    public List<LocalCacheEntryDetail> aggregateEntry(String serviceName, String area, String cacheName, 
                                                       String key, List<String> targetNodes) {
        if (serviceName == null || serviceName.isEmpty()) {
            logger.warn("[Aggregator] Service name is null or empty");
            return new ArrayList<>();
        }
        
        if (area == null || area.isEmpty() || cacheName == null || cacheName.isEmpty() 
                || key == null || key.isEmpty()) {
            logger.warn("[Aggregator] Invalid parameters for entry query, service: {}", serviceName);
            return new ArrayList<>();
        }
        
        if (registryClientFactory == null || restTemplate == null) {
            logger.error("[Aggregator] Required dependencies are not available");
            return new ArrayList<>();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            RegistryClient client = registryClientFactory.getRegistryClient(serviceName);
            if (client == null) {
                logger.warn("[Aggregator] No registry client found for service: {}", serviceName);
                return new ArrayList<>();
            }
            
            List<ServiceInstance> instances = client.listInstances(serviceName);
            
            if (targetNodes != null && !targetNodes.isEmpty()) {
                instances = instances.stream()
                        .filter(instance -> instance != null && instance.getNodeId() != null 
                                && targetNodes.contains(instance.getNodeId()))
                        .collect(Collectors.toList());
            }
            
            if (instances.isEmpty()) {
                logger.warn("[Aggregator] No instances found for service: {}", serviceName);
                return new ArrayList<>();
            }
            
            List<CompletableFuture<LocalCacheEntryDetail>> futures = instances.stream()
                    .map(instance -> queryEntryAsync(instance, area, cacheName, key))
                    .collect(Collectors.toList());
            
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            
            try {
                allFutures.get(totalTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.warn("[Aggregator] Some entry queries timed out or failed, service: {}", serviceName, e);
            }
            
            List<LocalCacheEntryDetail> results = new ArrayList<>();
            
            // 已完成的任务直接获取，未完成的任务再等待
            for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<LocalCacheEntryDetail> future = futures.get(i);
                ServiceInstance instance = instances.get(i);
                
                try {
                    LocalCacheEntryDetail entry = null;
                    // 如果已完成，直接获取结果，避免不必要的等待
                    if (future.isDone()) {
                        try {
                            entry = future.getNow(null);
                        } catch (Exception e) {
                            // getNow 不会抛出异常，但为了安全起见保留 try-catch
                            logger.debug("[Aggregator] Future is done but getNow failed for entry node: {}", 
                                    instance.getNodeId(), e);
                        }
                    } else {
                        // 未完成的任务，使用短超时获取（避免长时间等待）
                        try {
                            entry = future.get(50, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            // 超时或异常，记录日志
                            logger.debug("[Aggregator] Failed to get entry from node: {}, service: {}", 
                                    instance.getNodeId(), serviceName, e);
                        }
                    }
                    
                    if (entry != null) {
                        results.add(entry);
                    }
                } catch (Exception e) {
                    logger.debug("[Aggregator] Failed to get entry from node: {}, service: {}", 
                            instance.getNodeId() != null ? instance.getNodeId() : "unknown", serviceName, e);
                }
            }
            
            logger.debug("[Aggregator] Entry query completed, service: {}, results: {}, time: {}ms", 
                    serviceName, results.size(), System.currentTimeMillis() - startTime);
            
            return results;
            
        } catch (Exception e) {
            logger.error("[Aggregator] Entry query failed, service: {}", serviceName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 异步查询单个节点的缓存条目
     */
    private CompletableFuture<LocalCacheEntryDetail> queryEntryAsync(ServiceInstance instance, 
                                                                      String area, String cacheName, String key) {
        if (instance == null || area == null || cacheName == null || key == null) {
            return CompletableFuture.completedFuture(null);
        }

        return Futures8.withTimeout(
                CompletableFuture.supplyAsync(() -> {
                    try {
                        String url = String.format("%s%s?area=%s&name=%s&key=%s", 
                                instance.getUrl(), observerEntryPath, area, cacheName, key);
                        // 聚合查询默认只观测本地缓存（L0），避免触发 MultiLevelCache 的远端读取回填导致观测污染
                        url = url + "&level=L0";
                        LocalCacheEntryDetail entry = restTemplate.getForObject(url, LocalCacheEntryDetail.class);
                        if (entry != null && instance.getNodeId() != null) {
                            entry.setNodeId(instance.getNodeId());
                        }
                        return entry;
                    } catch (Exception e) {
                        String nodeId = instance.getNodeId() != null ? instance.getNodeId() : "unknown";
                        logger.debug("[Aggregator] Failed to query entry from node: {}, url: {}", nodeId, 
                                instance.getUrl() + observerEntryPath, e);
                        return null;
                    }
                }, executorService),
                perNodeTimeoutMs, TimeUnit.MILLISECONDS, scheduler
        );
    }
    
    /**
     * 检查多个节点的单个缓存条目是否一致
     * 
     * @param serviceName 服务名称
     * @param area 缓存区域
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param targetNodes 目标节点列表，为空则查询所有节点
     * @return 缓存一致性检查结果
     */
    public CacheConsistencyResult checkConsistency(String serviceName, String area, String cacheName, 
                                                   String key, List<String> targetNodes) {
        CacheConsistencyResult result = new CacheConsistencyResult(area, cacheName, key);
        
        if (serviceName == null || serviceName.isEmpty()) {
            logger.warn("[Aggregator] Service name is null or empty for consistency check");
            result.setConsistent(false);
            result.setConsistencyDetail("Service name is null or empty");
            result.setEntries(new ArrayList<>());
            result.setFailedNodes(new ArrayList<>());
            return result;
        }
        
        if (area == null || area.isEmpty() || cacheName == null || cacheName.isEmpty() 
                || key == null || key.isEmpty()) {
            logger.warn("[Aggregator] Invalid parameters for consistency check, service: {}", serviceName);
            result.setConsistent(false);
            result.setConsistencyDetail("Invalid parameters: area, cacheName or key is null or empty");
            result.setEntries(new ArrayList<>());
            result.setFailedNodes(new ArrayList<>());
            return result;
        }
        
        // 获取所有节点的缓存条目
        List<LocalCacheEntryDetail> entries = aggregateEntry(serviceName, area, cacheName, key, targetNodes);
        result.setEntries(entries);
        
        if (entries == null || entries.isEmpty()) {
            result.setConsistent(false);
            result.setConsistencyDetail("No entries found from any node");
            result.setFailedNodes(new ArrayList<>());
            return result;
        }
        
        // 统计查询失败的节点
        List<String> failedNodes = new ArrayList<>();
        if (registryClientFactory != null) {
            try {
                RegistryClient client = registryClientFactory.getRegistryClient(serviceName);
                if (client != null) {
                    List<ServiceInstance> instances = client.listInstances(serviceName);
                    if (targetNodes != null && !targetNodes.isEmpty()) {
                        instances = instances.stream()
                                .filter(instance -> instance != null && instance.getNodeId() != null 
                                        && targetNodes.contains(instance.getNodeId()))
                                .collect(Collectors.toList());
                    }
                    
                    List<String> queriedNodeIds = entries.stream()
                            .map(LocalCacheEntryDetail::getNodeId)
                            .filter(nodeId -> nodeId != null && !nodeId.isEmpty())
                            .collect(Collectors.toList());
                    
                    for (ServiceInstance instance : instances) {
                        if (instance != null && instance.getNodeId() != null 
                                && !queriedNodeIds.contains(instance.getNodeId())) {
                            failedNodes.add(instance.getNodeId());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("[Aggregator] Failed to get failed nodes for consistency check", e);
            }
        }
        result.setFailedNodes(failedNodes);
        
        // 检查一致性：比较所有节点的 value 是否相同
        boolean consistent = true;
        String firstValue = null;
        List<String> inconsistentNodes = new ArrayList<>();
        
        for (LocalCacheEntryDetail entry : entries) {
            if (entry == null) {
                consistent = false;
                continue;
            }
            
            String nodeId = entry.getNodeId();
            String value = entry.getValue();
            
            // 处理 null 值
            if (value == null) {
                value = "";
            }
            
            if (firstValue == null) {
                firstValue = value;
            } else {
                // 比较值是否一致
                if (!firstValue.equals(value)) {
                    consistent = false;
                    if (nodeId != null) {
                        inconsistentNodes.add(nodeId);
                    }
                }
            }
        }
        
        // 如果有查询失败的节点，也认为不一致
        if (!failedNodes.isEmpty()) {
            consistent = false;
        }
        
        result.setConsistent(consistent);
        
        // 生成一致性详情
        StringBuilder detail = new StringBuilder();
        if (consistent) {
            detail.append("All ").append(entries.size()).append(" node(s) have consistent cache values");
        } else {
            if (!inconsistentNodes.isEmpty()) {
                detail.append("Inconsistent values found in nodes: ").append(String.join(", ", inconsistentNodes));
            }
            if (!failedNodes.isEmpty()) {
                if (detail.length() > 0) {
                    detail.append("; ");
                }
                detail.append("Failed to query ").append(failedNodes.size()).append(" node(s): ")
                        .append(String.join(", ", failedNodes));
            }
            if (inconsistentNodes.isEmpty() && failedNodes.isEmpty()) {
                detail.append("Inconsistent cache values detected");
            }
        }
        result.setConsistencyDetail(detail.toString());
        
        logger.debug("[Aggregator] Consistency check completed, service: {}, area: {}, cacheName: {}, key: {}, " +
                        "consistent: {}, entries: {}, failedNodes: {}", 
                serviceName, area, cacheName, key, consistent, entries.size(), failedNodes.size());
        
        return result;
    }
    
    /**
     * 聚合失效多个节点的本地缓存
     * 
     * @param serviceName 服务名称
     * @param request 失效请求
     * @param targetNodes 目标节点列表，为空则失效所有节点
     * @return 失效结果列表（每个节点一个）
     */
    public List<InvalidateResult> aggregateInvalidate(String serviceName, InvalidateRequest request, 
                                                      List<String> targetNodes) {
        if (serviceName == null || serviceName.isEmpty()) {
            logger.warn("[Aggregator] Service name is null or empty for aggregate invalidate");
            return new ArrayList<>();
        }
        
        if (request == null) {
            logger.warn("[Aggregator] Invalidate request is null, service: {}", serviceName);
            return new ArrayList<>();
        }
        
        if (registryClientFactory == null || restTemplate == null) {
            logger.error("[Aggregator] Required dependencies are not available for aggregate invalidate");
            return new ArrayList<>();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            RegistryClient client = registryClientFactory.getRegistryClient(serviceName);
            if (client == null) {
                logger.warn("[Aggregator] No registry client found for service: {}", serviceName);
                return new ArrayList<>();
            }
            
            List<ServiceInstance> instances = client.listInstances(serviceName);
            
            if (targetNodes != null && !targetNodes.isEmpty()) {
                instances = instances.stream()
                        .filter(instance -> instance != null && instance.getNodeId() != null 
                                && targetNodes.contains(instance.getNodeId()))
                        .collect(Collectors.toList());
            }
            
            if (instances.isEmpty()) {
                logger.warn("[Aggregator] No instances found for service: {}", serviceName);
                return new ArrayList<>();
            }
            
            List<CompletableFuture<InvalidateResult>> futures = instances.stream()
                    .map(instance -> invalidateNodeAsync(instance, request))
                    .collect(Collectors.toList());
            
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            
            try {
                allFutures.get(totalTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.warn("[Aggregator] Some invalidate operations timed out or failed, service: {}", serviceName, e);
            }
            
            List<InvalidateResult> results = new ArrayList<>();
            
            // 已完成的任务直接获取，未完成的任务再等待
            for (int i = 0; i < futures.size(); i++) {
                CompletableFuture<InvalidateResult> future = futures.get(i);
                ServiceInstance instance = instances.get(i);
                
                try {
                    InvalidateResult result = null;
                    // 如果已完成，直接获取结果，避免不必要的等待
                    if (future.isDone()) {
                        try {
                            result = future.getNow(null);
                        } catch (Exception e) {
                            logger.debug("[Aggregator] Future is done but getNow failed for invalidate node: {}", 
                                    instance.getNodeId(), e);
                        }
                    } else {
                        // 未完成的任务，使用短超时获取（避免长时间等待）
                        try {
                            result = future.get(50, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            logger.debug("[Aggregator] Failed to get invalidate result from node: {}, service: {}", 
                                    instance.getNodeId(), serviceName, e);
                        }
                    }
                    
                    if (result != null) {
                        // 设置节点 ID
                        if (instance.getNodeId() != null) {
                            result.setNodeId(instance.getNodeId());
                        }
                        results.add(result);
                    } else {
                        // 创建失败结果
                        InvalidateResult failedResult = new InvalidateResult(
                                request.getArea(), request.getCacheName(), request.getKey());
                        if (instance.getNodeId() != null) {
                            failedResult.setNodeId(instance.getNodeId());
                        }
                        failedResult.setSuccess(false);
                        failedResult.setErrorMessage("Failed to invalidate cache on this node");
                        results.add(failedResult);
                    }
                } catch (Exception e) {
                    logger.debug("[Aggregator] Failed to get invalidate result from node: {}, service: {}", 
                            instance.getNodeId() != null ? instance.getNodeId() : "unknown", serviceName, e);
                    // 创建失败结果
                    InvalidateResult failedResult = new InvalidateResult(
                            request.getArea(), request.getCacheName(), request.getKey());
                    if (instance.getNodeId() != null) {
                        failedResult.setNodeId(instance.getNodeId());
                    }
                    failedResult.setSuccess(false);
                    failedResult.setErrorMessage("Exception: " + e.getMessage());
                    results.add(failedResult);
                }
            }
            
            logger.debug("[Aggregator] Aggregate invalidate completed, service: {}, results: {}, time: {}ms", 
                    serviceName, results.size(), System.currentTimeMillis() - startTime);
            
            return results;
            
        } catch (Exception e) {
            logger.error("[Aggregator] Aggregate invalidate failed, service: {}", serviceName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 异步失效单个节点的缓存
     */
    private CompletableFuture<InvalidateResult> invalidateNodeAsync(ServiceInstance instance, 
                                                                    InvalidateRequest request) {
        if (instance == null || request == null) {
            InvalidateResult failedResult = new InvalidateResult(
                    request != null ? request.getArea() : null,
                    request != null ? request.getCacheName() : null,
                    request != null ? request.getKey() : null);
            failedResult.setSuccess(false);
            failedResult.setErrorMessage("Instance or request is null");
            return CompletableFuture.completedFuture(failedResult);
        }

        return Futures8.withTimeout(
                CompletableFuture.supplyAsync(() -> {
                    try {
                        String url = instance.getUrl() + observerInvalidatePath;
                        InvalidateResult result = restTemplate.postForObject(url, request, InvalidateResult.class);
                        if (result == null) {
                            result = new InvalidateResult(request.getArea(), request.getCacheName(), request.getKey());
                            result.setSuccess(false);
                            result.setErrorMessage("No response from node");
                        }
                        return result;
                    } catch (Exception e) {
                        String nodeId = instance.getNodeId() != null ? instance.getNodeId() : "unknown";
                        logger.warn("[Aggregator] Failed to invalidate cache on node: {}, url: {}", nodeId, 
                                instance.getUrl() + observerInvalidatePath, e);
                        InvalidateResult failedResult = new InvalidateResult(
                                request.getArea(), request.getCacheName(), request.getKey());
                        failedResult.setSuccess(false);
                        failedResult.setErrorMessage("Exception: " + e.getMessage());
                        return failedResult;
                    }
                }, executorService),
                perNodeTimeoutMs, TimeUnit.MILLISECONDS, scheduler
        );
    }
    
    private AggregateResult createErrorResult(Exception e) {
        AggregateResult result = new AggregateResult();
        result.setResults(new ArrayList<>());
        result.setFailedNodes(new ArrayList<>());
        result.setPartial(true);
        return result;
    }
}
