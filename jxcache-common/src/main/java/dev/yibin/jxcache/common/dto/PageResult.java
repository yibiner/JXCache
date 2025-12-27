package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 分页结果
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> data;

    private long total;

    private int pageNo;

    private int pageSize;

    private int totalPages;

    public PageResult() {
    }

    public PageResult(List<T> data, long total, int pageNo, int pageSize) {
        this.data = data;
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageResult<?> that = (PageResult<?>) o;
        return total == that.total
                && pageNo == that.pageNo
                && pageSize == that.pageSize
                && totalPages == that.totalPages
                && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, total, pageNo, pageSize, totalPages);
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "total=" + total +
                ", pageNo=" + pageNo +
                ", pageSize=" + pageSize +
                ", totalPages=" + totalPages +
                '}';
    }
}
