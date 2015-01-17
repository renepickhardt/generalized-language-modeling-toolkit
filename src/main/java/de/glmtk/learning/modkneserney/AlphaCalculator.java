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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Status;
import de.glmtk.counts.AlphaCount;
import de.glmtk.counts.Discount;
import de.glmtk.files.AlphaCountWriter;
import de.glmtk.files.CountsReader;
import de.glmtk.files.DiscountReader;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public class AlphaCalculator {
    private static final Logger LOGGER = LogManager.getFormatterLogger(AlphaCalculator.class);

    private class Thread implements Callable<Object> {
        private Pattern pattern;
        private Pattern denomPattern;
        private Path countFile;
        private Path denomCountFile;
        private Path alphaFile;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                pattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (pattern == null)
                    continue;

                LOGGER.debug("Calculating pattern '%s'.", pattern);

                denomPattern = pattern.set(pattern.size() - 1,
                        pattern.isAbsolute()
                                ? PatternElem.SKP
                                : PatternElem.WSKP);
                if (pattern.isAbsolute()) {
                    countFile = absoluteDir.resolve(pattern.toString());
                    denomCountFile = absoluteDir.resolve(denomPattern.toString());
                } else {
                    countFile = continuationDir.resolve(pattern.toString());
                    denomCountFile = continuationDir.resolve(denomPattern.toString());
                }
                alphaFile = alphaDir.resolve(pattern.toString());

                calculateAlphasForPattern();

                status.addModelAlpha(Constants.MODEL_MODKNESERNEY_NAME, pattern);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void calculateAlphasForPattern() throws Exception {
            try (CountsReader reader = new CountsReader(countFile,
                    Constants.CHARSET, readerMemory / 2);
                    CountsReader denomReader = new CountsReader(denomCountFile,
                            Constants.CHARSET, readerMemory / 2);
                    AlphaCountWriter writer = new AlphaCountWriter(alphaFile,
                            Constants.CHARSET, writerMemory)) {
                while (true) {
                    if (reader.readLine() == null)
                        break;

                    String sequence = reader.getSequence();
                    List<String> split = StringUtils.splitAtChar(sequence, ' ');
                    if (pattern.isAbsolute())
                        split.set(split.size() - 1, PatternElem.SKP_WORD);
                    else
                        split.set(split.size() - 1, PatternElem.WSKP_WORD);
                    String denomSequence = StringUtils.join(split, " ");

                    String denomReaderSeq = denomReader.getSequence();
                    while (denomReaderSeq == null
                            || !denomReaderSeq.equals(denomSequence)) {
                        if (denomReader.isEof()
                                || (denomReaderSeq != null && denomReaderSeq.compareTo(denomSequence) > 0))
                            throw new Exception(
                                    String.format(
                                            "Could not find denominator Sequence '%s'.",
                                            denomSequence));

                        denomReader.readLine();
                        denomReaderSeq = denomReader.getSequence();
                    }

                    long sequenceCount = reader.getCount();
                    long denomSequenceCount = denomReader.getCount();

                    double normal = (double) sequenceCount / denomSequenceCount;
                    double discounted = 0.0;

                    writer.append(sequence, new AlphaCount(normal, discounted));
                }
            }
        }
    }

    private Config config;

    private Progress progress;
    private Status status;
    private Path absoluteDir;
    private Path continuationDir;
    private Path alphaDir;
    private Path discountsFile;
    private BlockingQueue<Pattern> patternQueue;
    private int readerMemory;
    private int writerMemory;
    private Map<Pattern, Discount> discounts;

    public AlphaCalculator(Config config) {
        this.config = config;
    }

    public void calculateAlphas(Status status,
                                Set<Pattern> patterns,
                                Path absoluteDir,
                                Path continuationDir,
                                Path alphaDir,
                                Path discountsFile) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_ALPHAS);

        LOGGER.debug("patterns = %s", patterns);

        LOGGER.debug("Filtering patterns.");
        Set<Pattern> remaining = new HashSet<>();
        for (Pattern pattern : patterns) {
            if (pattern.containsAll(Arrays.asList(PatternElem.SKP,
                    PatternElem.WSKP)))
                continue;
            Pattern denomPattern = pattern.set(pattern.size() - 1,
                    pattern.isAbsolute() ? PatternElem.SKP : PatternElem.WSKP);
            if (!status.getCounted().contains(denomPattern))
                continue;
            if (pattern.get(pattern.size() - 1) == PatternElem.CNT)
                remaining.add(pattern);
        }
        patterns = remaining;
        LOGGER.debug("Remaining patterns = %s", patterns);

        if (patterns.isEmpty())
            return;

        Files.createDirectories(alphaDir);

        this.status = status;
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        this.alphaDir = alphaDir;
        this.discountsFile = discountsFile;
        patternQueue = new LinkedBlockingQueue<>(patterns);
        calculateMemory();
        loadDiscounts();

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        progress = OUTPUT.newProgress(patterns.size());
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
