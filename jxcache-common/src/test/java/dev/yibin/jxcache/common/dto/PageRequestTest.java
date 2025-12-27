package dev.yibin.jxcache.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageRequest 单元测试
 */
class PageRequestTest {
    
    @Test
    void testDefaultConstructor() {
        PageRequest request = new PageRequest();
        assertThat(request.getPageNo()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(20);
        assertThat(request.getOffset()).isEqualTo(0);
    }
    
    @Test
    void testParameterizedConstructor() {
        PageRequest request = new PageRequest(2, 10);
        assertThat(request.getPageNo()).isEqualTo(2);
        assertThat(request.getPageSize()).isEqualTo(10);
        assertThat(request.getOffset()).isEqualTo(10);
    }
    
    @Test
    void testSettersAndGetters() {
        PageRequest request = new PageRequest();
        request.setPageNo(3);
        request.setPageSize(15);
        assertThat(request.getPageNo()).isEqualTo(3);
        assertThat(request.getPageSize()).isEqualTo(15);
        assertThat(request.getOffset()).isEqualTo(30);
    }
    
    @Test
    void testOffsetCalculation() {
        // Test various page numbers
        PageRequest request1 = new PageRequest(1, 20);
        assertThat(request1.getOffset()).isEqualTo(0);
        
        PageRequest request2 = new PageRequest(2, 20);
        assertThat(request2.getOffset()).isEqualTo(20);
        
        PageRequest request3 = new PageRequest(3, 20);
        assertThat(request3.getOffset()).isEqualTo(40);
    }
    
    @Test
    void testToString() {
        PageRequest request = new PageRequest(2, 10);
        String result = request.toString();
        assertThat(result).contains("PageRequest");
        assertThat(result).contains("pageNo=2");
        assertThat(result).contains("pageSize=10");
    }
}
