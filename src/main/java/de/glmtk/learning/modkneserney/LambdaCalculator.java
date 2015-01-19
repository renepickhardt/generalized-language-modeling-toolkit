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

package de.glmtk.learning.modkneserney;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.counts.Discount;
import de.glmtk.files.DiscountReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.ThreadUtils;

public class LambdaCalculator {
    private static final Logger LOGGER = Logger.get(LambdaCalculator.class);

    private class Thread implements Callable<Object> {
        private Pattern pattern;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                pattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (pattern == null)
                    continue;

                LOGGER.debug("Calculating pattern '%s'.", pattern);

                // stuff

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }
    }

    private Config config;

    private Progress progress;
    private Status status;
    private Path lambdaDir;
    private Path discountsFile;
    private BlockingQueue<Pattern> patternQueue;
    private int readerMemory;
    private int writerMemory;
    private Map<Pattern, Discount> discounts;

    public LambdaCalculator(Config config) {
        this.config = config;
    }

    public void calculateLambdas(Status status,
                                 Set<Pattern> patterns,
                                 Path lambdaDir,
                                 Path discountsFile) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_LAMBDAS);

        LOGGER.debug("patterns = %s", patterns);

        if (patterns.isEmpty())
            return;

        Files.createDirectories(lambdaDir);

        this.status = status;
        this.lambdaDir = lambdaDir;
        this.discountsFile = discountsFile;
        patternQueue = new PriorityBlockingQueue<>(patterns);
        calculateMemory();
        loadDiscounts();

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        progress = OUTPUT.newProgress(patternQueue.size());
        ThreadUtils.executeThreads(config.getNumberOfThreads(), threads);
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();

        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }

    private void loadDiscounts() throws IOException {
        discounts = new HashMap<>();
        try (DiscountReader reader = new DiscountReader(discountsFile,
                Constants.CHARSET)) {
            while (reader.readLine() != null) {
                Pattern pattern = reader.getPattern();
                Discount discount = reader.getDiscount();
                discounts.put(pattern, discount);
            }
        }
    }
}
