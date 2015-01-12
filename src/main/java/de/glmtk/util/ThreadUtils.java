package de.glmtk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadUtils {
    public static <T> List<T> executeThreads(int nThreads,
            List<Callable<T>> threads) throws Exception {
        CancellingThreadPoolExecutor threadPool = new CancellingThreadPoolExecutor(
                nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        List<Future<T>> futures = new ArrayList<>(threads.size());
        for (Callable<T> callable : threads)
            futures.add(threadPool.submit(callable));
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        threadPool.rethrowIfException();

        List<T> result = new ArrayList<>(threads.size());
        for (Future<T> future : futures)
            result.add(future.get());
        return result;
    }

    public static int executeProcess(final Process p,
                                     long timeout,
                                     TimeUnit unit) throws InterruptedException {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                p.waitFor();
                return p.exitValue();
            }
        };

        Future<Integer> future = Executors.newSingleThreadExecutor().submit(
                callable);

        try {
            int exitValue = future.get(timeout, unit);
            System.out.println("exitValue=" + exitValue);
            return exitValue;
        } catch (ExecutionException e) {
            // Should not happen
            throw new RuntimeException(e.getCause());
        } catch (TimeoutException e) {
            p.destroy();
            System.out.println("throw=1");
            return 1;
        }
    }
}
