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
import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.SKP_WORD;
import static de.glmtk.common.PatternElem.WSKP;
import static de.glmtk.common.PatternElem.WSKP_WORD;
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

    private static Set<Pattern> filterPatterns(Status status,
                                               Set<Pattern> patterns) {
        Set<Pattern> counted = status.getCounted();

        Set<Pattern> result = new HashSet<>();
        for (Pattern numPattern : patterns) {
            if (numPattern.get(numPattern.size() - 1) != CNT)
                continue;

            if (numPattern.containsAll(Arrays.asList(PatternElem.SKP,
                    PatternElem.WSKP)))
                continue;

            if (!counted.contains(numPattern))
                continue;

            if (!counted.contains(getDenPattern(numPattern)))
                continue;

            if (numPattern.size() != 1
                    && !counted.contains(getHistPattern(numPattern)))
                continue;

            result.add(numPattern);
        }
        return result;
    }

    private static Pattern getDenPattern(Pattern numPattern) {
        PatternElem last = numPattern.isAbsolute() ? SKP : WSKP;
        return numPattern.set(numPattern.size() - 1, last);
    }

    private static Pattern getHistPattern(Pattern numPattern) {
        return numPattern.range(0, numPattern.size() - 1);
    }

    private class Thread implements Callable<Object> {
        private Pattern numPattern;
        private Pattern denPattern;
        private Pattern histPattern;
        private Path histCountFile;
        private Path numCountFile;
        private Path denCountFile;
        private Path alphaFile;
        private boolean checkHistory;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                numPattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (numPattern == null)
                    continue;

                LOGGER.debug("Calculating pattern '%s'.", numPattern);

                denPattern = getDenPattern(numPattern);
                histPattern = getHistPattern(numPattern);

                if (numPattern.isAbsolute())
                    numCountFile = denCountFile = histCountFile = absoluteDir;
                else
                    numCountFile = denCountFile = histCountFile = continuationDir;
                numCountFile = numCountFile.resolve(numPattern.toString());
                denCountFile = denCountFile.resolve(denPattern.toString());
                histCountFile = histCountFile.resolve(histPattern.toString());
                alphaFile = alphaDir.resolve(numPattern.toString());

                checkHistory = numPattern.size() != 1;

                calculateAlphasForPattern();

                status.addModelAlpha(Constants.MODEL_MODKNESERNEY_NAME,
                        numPattern);

                LOGGER.debug("Finished pattern '%s'.", numPattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void calculateAlphasForPattern() throws Exception {
            Discount discount = null;
            if (checkHistory)
                discount = discounts.get(histPattern);

            try (CountsReader numReader = new CountsReader(numCountFile,
                    Constants.CHARSET, readerMemory / 3);
                    CountsReader denReader = new CountsReader(denCountFile,
                            Constants.CHARSET, readerMemory / 3);
                    CountsReader histReader = !checkHistory
                            ? null
                                    : new CountsReader(histCountFile,
                                            Constants.CHARSET, readerMemory / 3);
                    AlphaCountWriter writer = new AlphaCountWriter(alphaFile,
                            Constants.CHARSET, writerMemory)) {
                while (numReader.readLine() != null) {
                    String numSequence = numReader.getSequence();
                    List<String> split = StringUtils.splitAtChar(numSequence,
                            ' ');

                    split.remove(split.size() - 1);
                    String histSequence = StringUtils.join(split, ' ');

                    split.add(numPattern.isAbsolute() ? SKP_WORD : WSKP_WORD);
                    String denSequence = StringUtils.join(split, ' ');

                    findSequenceInReader(denReader, denSequence);

                    double num = numReader.getCount();
                    double den = denReader.getCount();

                    double normal = num / den;
                    double discounted = Double.NaN;

                    if (checkHistory) {
                        findSequenceInReader(histReader, histSequence);

                        double d = discount.getForCount(histReader.getCount());
                        discounted = Math.max(num - d, 0.0) / den;
                    }

                    writer.append(numSequence, new AlphaCount(normal,
                            discounted));
                }
            }
        }

        private void findSequenceInReader(CountsReader reader,
                                          String sequence) throws Exception {
            String seq = reader.getSequence();
            while (seq == null || !seq.equals(sequence)) {
                if (reader.isEof()
                        || (seq != null && seq.compareTo(sequence) > 0))
                    throw new Exception(String.format(
                            "Could not find sequence '%s' in '%s'.", sequence,
                            reader.getFile()));

                reader.readLine();
                seq = reader.getSequence();
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
        Set<Pattern> remainingPatterns = filterPatterns(status, patterns);
        LOGGER.debug("Remaining patterns = %s", patterns);
        patterns = remainingPatterns;

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