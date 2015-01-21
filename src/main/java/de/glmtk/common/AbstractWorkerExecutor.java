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

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;
import static de.glmtk.util.PrintUtils.logHeader;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Output.Progress;
import de.glmtk.logging.Logger;
import de.glmtk.util.ThreadUtils;

public abstract class AbstractWorkerExecutor<T> {
    private static final Logger LOGGER = Logger.get(AbstractWorkerExecutor.class);

    protected abstract class Worker implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            LOGGER.debug("%s started.", getClass().getName());

            while (!queue.isEmpty()) {
                T object = queue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (object == null)
                    continue;

                work(object);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("%s finished.", getClass().getName());
            return null;
        }

        protected abstract void work(T object) throws Exception;
    }

    protected Config config;

    // Memory
    protected int readerMemory;
    protected int writerMemory;

    protected GlmtkPaths paths;
    protected Status status;

    private BlockingQueue<T> queue;
    private Progress progress;

    public AbstractWorkerExecutor(Config config) {
        this.config = config;
        calculateMemory();
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();

        LOGGER.debug(logHeader(getClass().getSimpleName() + " Memory"));
        LOGGER.debug("readerMemory : %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory : %s", humanReadableByteCount(writerMemory));
    }

    public final void run(Collection<T> objects,
                          GlmtkPaths paths,
                          Status status) throws Exception {
        this.paths = paths;
        this.status = status;

        objects = prepare(objects);

        if (objects == null || objects.isEmpty()) {
            LOGGER.debug("No objects given, no work to do.");
            return;
        }

        queue = createQueue(objects);
        progress = OUTPUT.newProgress(queue.size());

        Collection<? extends Worker> workers = createWorkers();
        ThreadUtils.executeThreads(workers.size(), workers);
    }

    protected abstract Collection<T> prepare(Collection<T> objects) throws Exception;

    protected BlockingQueue<T> createQueue(Collection<T> objects) {
        return new LinkedBlockingQueue<>(objects);
    }

    protected abstract Collection<? extends Worker> createWorkers();
}
