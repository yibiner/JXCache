package dev.yibin.jxcache.common.impl;

import com.alibaba.fastjson2.JSON;
import dev.yibin.jxcache.common.spi.ValuePreviewer;

/**
 * 默认值预览器实现
 *
 * <p>提供缓存值的预览功能，支持字符串截断和类型识别</p>
 */
public class DefaultValuePreviewer implements ValuePreviewer {

    @Override
    public String preview(Object value, int maxLength) {
        if (value == null) {
            return "null";
        }

        String valueStr;
        if (value instanceof String) {
            valueStr = (String) value;
        } else {
            try {
                // 尝试使用 FastJSON2 序列化
                valueStr = JSON.toJSONString(value);
            } catch (Exception e) {
                // 如果序列化失败，使用 toString 方法
                valueStr = value.toString();
            }
        }

        // 如果长度超过限制，进行截断
        if (valueStr.length() <= maxLength) {
            return valueStr;
        }

        return valueStr.substring(0, maxLength) + "...";
    }

    @Override
    public String getValueType(Object value) {
        if (value == null) {
            return "null";
        }
        return value.getClass().getSimpleName();
    }
}
