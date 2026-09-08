package com.tianma.xsmscode.core;

import java.util.concurrent.*;

/** Never spawn replacement threads for a Java regex which ignores interrupts. */
public final class BoundedParser {
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0,
            TimeUnit.MILLISECONDS, new SynchronousQueue<>(), runnable -> {
        Thread t = new Thread(runnable, "SmsCode-parser"); t.setDaemon(true); return t;
    }, new ThreadPoolExecutor.AbortPolicy());

    public <T> T evaluate(Callable<T> action, long timeoutMillis) {
        Future<T> future;
        try { future = executor.submit(action); }
        catch (RejectedExecutionException e) { return null; }
        try { return future.get(timeoutMillis, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
        catch (ExecutionException | TimeoutException e) { return null; }
        finally { if (!future.isDone()) future.cancel(true); }
    }
    public void close() { executor.shutdownNow(); }
}
