package de.glmtk.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPool that stops executing of all tasks as soon as one task throws an
 * exception, instead of waiting for remaining tasks to finish.
 */
public class CancellingThreadPoolExecutor extends ThreadPoolExecutor {

    private Throwable throwable = null;

    public CancellingThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public CancellingThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory);
    }

    public CancellingThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                handler);
    }

    public CancellingThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, handler);
    }

    /**
     * See {@link ThreadPoolExecutor#afterExecute(Runnable, Throwable)}.
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> f = (Future<?>) r;
                if (!f.isCancelled()) {
                    f.get();
                }
            } catch (InterruptedException | CancellationException e) {
                t = e;
            } catch (ExecutionException e) {
                t = e.getCause();
            }
        }

        if (throwable == null && t != null
                && !(t instanceof InterruptedException)) {
            shutdownNow();
            throwable = t;
        }
    }

    public void rethrowIfException() throws Exception {
        if (throwable != null) {
            if (throwable instanceof Error) {
                throw (Error) throwable;
            } else if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else {
                throw (Exception) throwable;
            }
        }
    }
}
