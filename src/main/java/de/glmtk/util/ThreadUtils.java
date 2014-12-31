package de.glmtk.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    public static void executeThreads(int nThreads, List<Runnable> threads)
            throws InterruptedException {
        ExecutorService executorService =
                Executors.newFixedThreadPool(nThreads);

        for (Runnable thread : threads) {
            executorService.execute(thread);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}
