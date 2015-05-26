/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadUtils {
    private ThreadUtils() {
    }

    public static <T> List<T> executeThreads(int nThreads,
            Collection<? extends Callable<T>> threads) throws Exception {
        CancellingThreadPoolExecutor threadPool = new CancellingThreadPoolExecutor(
                nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());

        List<Future<T>> futures = new ArrayList<>(threads.size());
        for (Callable<T> callable : threads)
            futures.add(threadPool.submit(callable));
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        threadPool.rethrowIfException();

        List<T> results = new ArrayList<>(threads.size());
        for (Future<T> future : futures)
            results.add(future.get());
        return results;
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

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> future = executorService.submit(callable);

            int exitValue = future.get(timeout, unit);
            return exitValue;
        } catch (TimeoutException e) {
            p.destroy();
            return 1;
        } catch (ExecutionException e) {
            // Should not happen
            throw new RuntimeException(e.getCause());
        } finally {
            executorService.shutdown();
        }
    }
}
