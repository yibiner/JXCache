package dev.yibin.jxcache.common.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultValuePreviewer 单元测试
 */
class DefaultValuePreviewerTest {
    
    private DefaultValuePreviewer valuePreviewer;
    
    @BeforeEach
    void setUp() {
        valuePreviewer = new DefaultValuePreviewer();
    }
    
    @Test
    void testPreviewWithNullValue() {
        String result = valuePreviewer.preview(null, 100);
        assertThat(result).isEqualTo("null");
    }
    
    @Test
    void testPreviewWithStringValue() {
        String value = "test string";
        String result = valuePreviewer.preview(value, 100);
        assertThat(result).isEqualTo(value);
    }
    
    @Test
    void testPreviewWithLongString() {
        String value = "this is a very long string that should be truncated";
        String result = valuePreviewer.preview(value, 10);
        assertThat(result).isEqualTo("this is a ...");
    }
    
    @Test
    void testPreviewWithObject() {
        TestObject obj = new TestObject("test", 123);
        String result = valuePreviewer.preview(obj, 100);
        assertThat(result).contains("test");
        assertThat(result).contains("123");
    }
    
    @Test
    void testGetValueTypeWithNull() {
        String result = valuePreviewer.getValueType(null);
        assertThat(result).isEqualTo("null");
    }
    
    @Test
    void testGetValueTypeWithString() {
        String result = valuePreviewer.getValueType("test");
        assertThat(result).isEqualTo("String");
    }
    
    @Test
    void testGetValueTypeWithObject() {
        String result = valuePreviewer.getValueType(new TestObject("test", 123));
        assertThat(result).isEqualTo("TestObject");
    }
    
    /**
     * 测试对象
     */
    private static class TestObject {
        private String name;
        private int value;
        
        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
        
        public String getName() {
            return name;
        }
        
        public int getValue() {
            return value;
        }
    }
}
