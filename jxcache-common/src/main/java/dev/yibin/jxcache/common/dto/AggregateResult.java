package dev.yibin.jxcache.common.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 聚合查询结果
 */
public class AggregateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<LocalCacheSnapshot> results;

    private List<String> failedNodes;

    private boolean partial;

    private long queryTime;

    private long totalTimeMs;

    public AggregateResult() {
        this.queryTime = System.currentTimeMillis();
    }

    public List<LocalCacheSnapshot> getResults() {
        return results;
    }

    public void setResults(List<LocalCacheSnapshot> results) {
        this.results = results;
    }

    public List<String> getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(List<String> failedNodes) {
        this.failedNodes = failedNodes;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(long queryTime) {
        this.queryTime = queryTime;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    public void setTotalTimeMs(long totalTimeMs) {
        this.totalTimeMs = totalTimeMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregateResult that = (AggregateResult) o;
        return partial == that.partial
                && queryTime == that.queryTime
                && totalTimeMs == that.totalTimeMs
                && Objects.equals(results, that.results)
                && Objects.equals(failedNodes, that.failedNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results, failedNodes, partial, queryTime, totalTimeMs);
    }

    @Override
    public String toString() {
        return "AggregateResult{" +
                "results=" + results +
                ", failedNodes=" + failedNodes +
                ", partial=" + partial +
                ", queryTime=" + queryTime +
                ", totalTimeMs=" + totalTimeMs +
                '}';
    }
}
