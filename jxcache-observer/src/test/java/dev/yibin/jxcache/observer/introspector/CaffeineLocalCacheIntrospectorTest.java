package dev.yibin.jxcache.observer.introspector;

import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.embedded.CaffeineCache;
import dev.yibin.jxcache.common.dto.LocalCacheEntryDetail;
import dev.yibin.jxcache.common.dto.LocalCacheSnapshot;
import dev.yibin.jxcache.common.dto.PageRequest;
import dev.yibin.jxcache.common.dto.QueryRequest;
import dev.yibin.jxcache.common.spi.ValuePreviewer;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * CaffeineLocalCacheIntrospector 单元测试（JDK8 / Mockito 4.1.0 兼容）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaffeineLocalCacheIntrospectorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaffeineLocalCacheIntrospectorTest.class);

    @Mock
    private CacheManager cacheManager; // com.alicp.jetcache.CacheManager

    @Mock
    private ValuePreviewer valuePreviewer;

    @Mock
    private CaffeineCache<Object, Object> jetcacheCaffeine; // JetCache 包装类

    @Spy
    @InjectMocks
    private CaffeineLocalCacheIntrospector introspector;

    @BeforeEach
    void setUp() {
        // 通用桩：值预览
        when(valuePreviewer.preview(any(), anyInt())).thenReturn("preview");
        when(valuePreviewer.getValueType(any())).thenReturn("String");
    }

    @Test
    void testScan_withPrefixFilter() {
        // 准备：构造一个原生 Caffeine 缓存并放入数据
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                Caffeine.newBuilder().build();

        nativeCache.put("key1", "value1");
        nativeCache.put("key2", "value2");
        nativeCache.put("prefix_key3", "value3");

        // JetCache 的 CacheManager 双参获取
        when(cacheManager.getCache("area1", "cache1")).thenReturn(jetcacheCaffeine);
        // 用 spy 覆盖被测类里“提取原生 Caffeine 缓存”的方法，避免反射不稳定
        doReturn(nativeCache).when(introspector).extractNativeCaffeineCache(jetcacheCaffeine);

        QueryRequest req = new QueryRequest();
        req.setArea("area1");
        req.setCacheName("cache1");
        req.setKeyPrefix("prefix_");
        req.setPageRequest(new PageRequest(1, 10));

        LocalCacheSnapshot snap = introspector.scan(jetcacheCaffeine, req);

        assertThat(snap).isNotNull();
        assertThat(snap.getArea()).isEqualTo("area1");
        assertThat(snap.getCacheName()).isEqualTo("cache1");
        assertThat(snap.getEntries()).hasSize(1);
        assertThat(snap.getEntries().get(0).getKey()).isEqualTo("prefix_key3");
        assertThat(snap.getTotal()).isEqualTo(1);
        assertThat(snap.isPartial()).isFalse();
    }

    @Test
    void testScan_withSharding() {
        // 构造 10 个 key
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                Caffeine.newBuilder().build();
        for (int i = 0; i < 10; i++) {
            nativeCache.put("key" + i, "value" + i);
        }

        when(cacheManager.getCache("area1", "cache1")).thenReturn(jetcacheCaffeine);
        doReturn(nativeCache).when(introspector).extractNativeCaffeineCache(jetcacheCaffeine);

        QueryRequest req = new QueryRequest();
        req.setArea("area1");
        req.setCacheName("cache1");
        req.setShard(0);
        req.setTotalShards(2);
        req.setPageRequest(new PageRequest(1, 10));

        LocalCacheSnapshot snap = introspector.scan(jetcacheCaffeine, req);
        LOGGER.info(snap.toString());

        assertThat(snap).isNotNull();
        assertThat(snap.isPartial()).isTrue();
        // 稳定排序 + 取模分片后，第 0 片应有 5 条
        assertThat(snap.getTotal()).isEqualTo(5);
    }

    @Test
    void testFindEntry_returnsFullValue() {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                Caffeine.newBuilder().build();
        nativeCache.put("key1", "value1");

        when(cacheManager.getCache("area1", "cache1")).thenReturn(jetcacheCaffeine);
        doReturn(nativeCache).when(introspector).extractNativeCaffeineCache(jetcacheCaffeine);
        when(valuePreviewer.preview(eq("value1"), anyInt())).thenReturn("value1");
        // Mock Cache.get() 方法
        when(jetcacheCaffeine.get("key1")).thenReturn("value1");

        Optional<LocalCacheEntryDetail> result = introspector.findEntry(jetcacheCaffeine, "area1", "cache1", "key1");

        assertThat(result).isPresent();
        LocalCacheEntryDetail detail = result.get();
        assertThat(detail.getKey()).isEqualTo("key1");
        assertThat(detail.getValue()).isEqualTo("value1");
        assertThat(detail.isTruncated()).isFalse();
        assertThat(detail.getValueLength()).isEqualTo("value1".length());
    }

    @Test
    void testFindEntry_whenMissingReturnsEmpty() {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                Caffeine.newBuilder().build();
        nativeCache.put("key1", "value1");

        when(cacheManager.getCache("area1", "cache1")).thenReturn(jetcacheCaffeine);
        doReturn(nativeCache).when(introspector).extractNativeCaffeineCache(jetcacheCaffeine);
        // Mock Cache.get() 返回 null（未找到）
        when(jetcacheCaffeine.get("missing")).thenReturn(null);

        Optional<LocalCacheEntryDetail> result = introspector.findEntry(jetcacheCaffeine, "area1", "cache1", "missing");

        assertThat(result).isNotPresent();
    }
}
