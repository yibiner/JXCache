package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * 分页请求参数
 */
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pageNo = 1;

    private int pageSize = 20;

    public PageRequest() {
    }

    public PageRequest(int pageNo, int pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getOffset() {
        return Math.max(pageNo - 1, 0) * Math.max(pageSize, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageRequest that = (PageRequest) o;
        return pageNo == that.pageNo && pageSize == that.pageSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNo, pageSize);
    }

    @Override
    public String toString() {
        return "PageRequest{" +
                "pageNo=" + pageNo +
                ", pageSize=" + pageSize +
                '}';
    }
}
