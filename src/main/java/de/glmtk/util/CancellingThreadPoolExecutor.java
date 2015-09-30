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

    public CancellingThreadPoolExecutor(int corePoolSize,
                                        int maximumPoolSize,
                                        long keepAliveTime,
                                        TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public CancellingThreadPoolExecutor(int corePoolSize,
                                        int maximumPoolSize,
                                        long keepAliveTime,
                                        TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue,
                                        ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            threadFactory);
    }

    public CancellingThreadPoolExecutor(int corePoolSize,
                                        int maximumPoolSize,
                                        long keepAliveTime,
                                        TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue,
                                        RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            handler);
    }

    public CancellingThreadPoolExecutor(int corePoolSize,
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
    protected void afterExecute(Runnable r,
                                Throwable t) {
        super.afterExecute(r, t);

        if (t == null && r instanceof Future) {
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
