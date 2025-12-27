package dev.yibin.jxcache.observer.introspector;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.embedded.LinkedHashMapCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LinkedHashMap 本地缓存内省器
 * 支持 LinkedHashMapCache 类型的本地缓存扫描和查询
 */
@Component
@ConditionalOnClass(name = "com.alicp.jetcache.embedded.LinkedHashMapCache")
public class LinkedHashMapLocalCacheIntrospector extends AbstractLocalCacheIntrospector {
    
    @Override
    public boolean supports(Cache<?, ?> cache) {
        return cache instanceof LinkedHashMapCache;
    }
    
    @Override
    protected Iterator<Map.Entry<Object, Object>> getEntryIterator(Cache<Object, Object> cache) {
        if (!(cache instanceof LinkedHashMapCache)) {
            return null;
        }
        
        LinkedHashMap<Object, Object> nativeMap = extractNativeLinkedHashMap((LinkedHashMapCache<Object, Object>) cache);
        if (nativeMap == null) {
            return null;
        }
        
        // LinkedHashMap 的 entrySet().iterator() 是有序的（按插入顺序）
        return nativeMap.entrySet().iterator();
    }
    
    /**
     * 提取原生 LinkedHashMap
     * 通过反射获取 LinkedHashMapCache 内部的原生 LinkedHashMap 实例
     */
    @SuppressWarnings("unchecked")
    private LinkedHashMap<Object, Object> extractNativeLinkedHashMap(LinkedHashMapCache<Object, Object> jetCache) {
        try {
            // LinkedHashMapCache 内部使用 LinkedHashMap 存储数据
            // 字段名可能是 "map" 或 "cache"，尝试多个可能的字段名
            Field[] fields = jetCache.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (Map.class.isAssignableFrom(field.getType()) || 
                    LinkedHashMap.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object nativeMap = field.get(jetCache);
                    if (nativeMap instanceof LinkedHashMap) {
                        return (LinkedHashMap<Object, Object>) nativeMap;
                    } else if (nativeMap instanceof Map) {
                        // 如果不是 LinkedHashMap，尝试包装为 LinkedHashMap（保持顺序）
                        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
                        result.putAll((Map<Object, Object>) nativeMap);
                        return result;
                    }
                }
            }
            
            // 如果字段查找失败，尝试直接通过 getDeclaredField 查找常见字段名
            String[] fieldNames = {"map", "cache", "data", "storage"};
            for (String fieldName : fieldNames) {
                try {
                    Field f = jetCache.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object nativeMap = f.get(jetCache);
                    if (nativeMap instanceof LinkedHashMap) {
                        return (LinkedHashMap<Object, Object>) nativeMap;
                    } else if (nativeMap instanceof Map) {
                        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
                        result.putAll((Map<Object, Object>) nativeMap);
                        return result;
                    }
                } catch (NoSuchFieldException ignored) {
                    // 继续尝试下一个字段名
                }
            }
        } catch (IllegalAccessException | SecurityException e) {
            // 反射失败，返回 null
        }
        return null;
    }
}

