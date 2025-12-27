package dev.yibin.jxcache.aggregator.service;


import java.util.concurrent.*;

/**
 * Futures8
 *
 **/
public final class Futures8 {
    private Futures8() {
    }

    public static <T> CompletableFuture<T> withTimeout(
            CompletableFuture<T> original,
            long timeout, TimeUnit unit,
            ScheduledExecutorService scheduler) {

        final CompletableFuture<T> result = new CompletableFuture<>();
        final ScheduledFuture<?> timeoutTask = scheduler.schedule(
                () -> result.completeExceptionally(new TimeoutException(
                        "Timeout after " + timeout + " " + unit)),
                timeout, unit);

        original.whenComplete((v, ex) -> {
            try {
                if (ex == null) result.complete(v);
                else result.completeExceptionally(ex);
            } finally {
                timeoutTask.cancel(false);
            }
        });
        return result;
    }
}
