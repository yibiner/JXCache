package dev.yibin.jxcache.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResult 单元测试
 */
class PageResultTest {
    
    @Test
    void testDefaultConstructor() {
        PageResult<String> result = new PageResult<>();
        assertThat(result.getData()).isNull();
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getPageNo()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }
    
    @Test
    void testParameterizedConstructor() {
        List<String> data = Arrays.asList("item1", "item2", "item3");
        long total = 25L;
        int pageNo = 2;
        int pageSize = 10;
        PageResult<String> result = new PageResult<>(data, total, pageNo, pageSize);
        assertThat(result.getData()).isEqualTo(data);
        assertThat(result.getTotal()).isEqualTo(total);
        assertThat(result.getPageNo()).isEqualTo(pageNo);
        assertThat(result.getPageSize()).isEqualTo(pageSize);
        assertThat(result.getTotalPages()).isEqualTo(3); // Math.ceil(25/10) = 3
    }
    
    @Test
    void testTotalPagesCalculation() {
        // Test exact division
        PageResult<String> result1 = new PageResult<>(Arrays.asList("a", "b"), 20L, 1, 10);
        assertThat(result1.getTotalPages()).isEqualTo(2);
        
        // Test with remainder
        PageResult<String> result2 = new PageResult<>(Arrays.asList("a", "b"), 25L, 1, 10);
        assertThat(result2.getTotalPages()).isEqualTo(3);
        
        // Test with zero total
        PageResult<String> result3 = new PageResult<>(Arrays.asList(), 0L, 1, 10);
        assertThat(result3.getTotalPages()).isEqualTo(0);
    }
    
    @Test
    void testSettersAndGetters() {
        PageResult<String> result = new PageResult<>();
        List<String> data = Arrays.asList("test1", "test2");
        result.setData(data);
        result.setTotal(100L);
        result.setPageNo(5);
        result.setPageSize(20);
        result.setTotalPages(5);
        assertThat(result.getData()).isEqualTo(data);
        assertThat(result.getTotal()).isEqualTo(100L);
        assertThat(result.getPageNo()).isEqualTo(5);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getTotalPages()).isEqualTo(5);
    }
    
    @Test
    void testToString() {
        PageResult<String> result = new PageResult<>(Arrays.asList("a", "b"), 10L, 1, 5);
        String resultStr = result.toString();
        assertThat(resultStr).contains("PageResult");
        assertThat(resultStr).contains("total=10");
        assertThat(resultStr).contains("pageNo=1");
        assertThat(resultStr).contains("pageSize=5");
    }
}
