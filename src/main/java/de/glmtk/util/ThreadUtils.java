package de.glmtk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    public static <T> List<T> executeThreads(int nThreads,
                                             List<Callable<T>> threads) throws Exception {
        CancellingThreadPoolExecutor threadPool = new CancellingThreadPoolExecutor(
                nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        List<Future<T>> futures = new ArrayList<Future<T>>(threads.size());
        for (Callable<T> callable : threads)
            futures.add(threadPool.submit(callable));
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        threadPool.rethrowIfException();

        List<T> result = new ArrayList<T>(threads.size());
        for (Future<T> future : futures)
            result.add(future.get());
        return result;
    }
}
