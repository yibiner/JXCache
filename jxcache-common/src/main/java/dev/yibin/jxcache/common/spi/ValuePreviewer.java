package dev.yibin.jxcache.common.spi;

/**
 * 值预览器 - 用于截断缓存值显示
 * @author Yibin
 * @since 1.0.0
 */
public interface ValuePreviewer {
    
    /**
     * 预览值
     * 
     * @param value 原始值
     * @param maxLength 最大长度
     * @return 截断后的预览值
     */
    String preview(Object value, int maxLength);
    
    /**
     * 获取值类型
     * 
     * @param value 原始值
     * @return 值类型字符串
     */
    String getValueType(Object value);
}
