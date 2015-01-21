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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Cache;
import de.glmtk.common.CacheBuilder;
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
import de.glmtk.logging.Logger;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public class AlphaCalculator {
    private static final Logger LOGGER = Logger.get(AlphaCalculator.class);

    private static Set<Pattern> filterPatterns(Collection<Pattern> counted,
                                               Set<Pattern> patterns) {
        Set<Pattern> result = new HashSet<>();
        for (Pattern numPattern : patterns) {
            if (numPattern.get(numPattern.size() - 1) != CNT
                    || numPattern.containsAll(Arrays.asList(SKP, WSKP))
                    || !counted.contains(numPattern)
                    || !counted.contains(getDenPattern(numPattern))
                    || (numPattern.size() != 1 && !counted.contains(getHistPattern(numPattern))))
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

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                numPattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (numPattern == null)
                    continue;

                LOGGER.debug("Calculating pattern '%s'.", numPattern);

                calculateAlphasForPattern();

                status.addAlpha(Constants.MODEL_MODKNESERNEY_NAME, numPattern);

                LOGGER.debug("Finished pattern '%s'.", numPattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void calculateAlphasForPattern() throws Exception {
            Pattern denPattern = getDenPattern(numPattern);
            Pattern histPattern = getHistPattern(numPattern);

            Path numCountFile, denCountFile, histCountFile;
            if (numPattern.isAbsolute())
                numCountFile = denCountFile = histCountFile = absoluteDir;
            else
                numCountFile = denCountFile = histCountFile = continuationDir;
            numCountFile = numCountFile.resolve(numPattern.toString());
            denCountFile = denCountFile.resolve(denPattern.toString());
            histCountFile = histCountFile.resolve(histPattern.toString());

            Path alphaFile = alphaDir.resolve(numPattern.toString());

            boolean checkHistory = numPattern.size() != 1;

            Discount discount = null;
            if (checkHistory)
                discount = cache.getDiscount(Constants.MODEL_MODKNESERNEY_NAME,
                        histPattern);

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

                    denReader.forwardToSequence(denSequence);

                    long num = numReader.getCount();
                    long den = denReader.getCount();

                    double normal = (double) num / den;
                    double discounted = Double.NaN;

                    if (checkHistory) {
                        histReader.forwardToSequence(histSequence);

                        double d = discount.getForCount(histReader.getCount());
                        discounted = Math.max(num - d, 0.0) / den;
                    }

                    writer.append(numSequence, new AlphaCount(normal,
                            discounted));
                }
            }
        }
    }

    private Config config;
    private int readerMemory;
    private int writerMemory;

    private Path absoluteDir;
    private Path continuationDir;
    private Path alphaDir;
    private Status status;
    private BlockingQueue<Pattern> patternQueue;
    private Cache cache;
    private Progress progress;

    public AlphaCalculator(Config config) {
        this.config = config;

        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();

        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }

    public void calculateAlphas(GlmtkPaths paths,
                                Status status,
                                Set<Pattern> patterns) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_ALPHAS);

        patterns = filterPatterns(status.getCounted(), patterns);
        patterns.removeAll(status.getAlphas(Constants.MODEL_MODKNESERNEY_NAME));
        if (patterns.isEmpty())
            return;

        absoluteDir = paths.getAbsoluteDir();
        continuationDir = paths.getContinuationDir();
        alphaDir = paths.getModKneserNeyAlphaDir();
        this.status = status;
        patternQueue = new LinkedBlockingQueue<>(patterns);
        cache = new CacheBuilder(paths).withDiscounts(
                Constants.MODEL_MODKNESERNEY_NAME).build();

        Files.createDirectories(alphaDir);

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        progress = OUTPUT.newProgress(patternQueue.size());
        ThreadUtils.executeThreads(config.getNumberOfThreads(), threads);
    }
}
