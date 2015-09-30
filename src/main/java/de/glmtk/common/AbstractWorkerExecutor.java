/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen
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

package de.glmtk.common;

import static de.glmtk.util.PrintUtils.humanReadableByteCount;
import static de.glmtk.util.PrintUtils.logHeader;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.glmtk.Constants;
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.util.ThreadUtils;


public abstract class AbstractWorkerExecutor<T> {
    private static final Logger LOGGER =
        Logger.get(AbstractWorkerExecutor.class);

    protected abstract class Worker implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            LOGGER.debug("%s$%s started.",
                AbstractWorkerExecutor.this.getClass().getSimpleName(),
                getClass().getSimpleName());

            while (!queue.isEmpty()) {
                T obj;
                int objNo;
                synchronized (queue) {
                    if ((obj = queue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS)) == null) {
                        continue;
                    }
                    objNo = objectNo++;
                }

                work(obj, objNo);

                synchronized (progressBar) {
                    progressBar.increase();
                }
            }

            LOGGER.debug("%s$%s finished.",
                AbstractWorkerExecutor.this.getClass().getSimpleName(),
                getClass().getSimpleName());
            return null;
        }

        protected abstract void work(T object,
                                     int objectNo) throws Exception;
    }

    protected Config config;

    // Memory
    protected int readerMemory;
    protected int writerMemory;

    private int objectNo;
    private BlockingQueue<T> queue;
    private ProgressBar progressBar;

    public AbstractWorkerExecutor(Config config) {
        this.config = config;
        calculateMemory();
    }

    protected void calculateMemory() {
        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();

        LOGGER.debug(logHeader(getClass().getSimpleName() + " Memory"));
        LOGGER.debug("readerMemory : %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory : %s", humanReadableByteCount(writerMemory));
    }

    protected void work(Collection<T> objects,
                        ProgressBar progressBar) throws Exception {
        if (objects == null || objects.isEmpty()) {
            LOGGER.debug("No objects given, no work to do.");
            return;
        }

        objectNo = 0;
        queue = createQueue(objects);
        this.progressBar = progressBar;
        this.progressBar.total(queue.size());

        Collection<? extends Worker> workers = createWorkers();
        ThreadUtils.executeThreads(workers.size(), workers);
    }

    protected BlockingQueue<T> createQueue(Collection<T> objects) {
        return new LinkedBlockingQueue<>(objects);
    }

    protected abstract Collection<? extends Worker> createWorkers();
}
