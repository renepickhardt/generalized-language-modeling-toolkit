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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discount;
import de.glmtk.counts.LambdaCount;
import de.glmtk.counts.LambdaCounts;
import de.glmtk.files.CountsReader;
import de.glmtk.files.DiscountReader;
import de.glmtk.files.LambdaCountsReader;
import de.glmtk.files.LambdaCountsWriter;
import de.glmtk.logging.Logger;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public class LambdaCalculator {
    private static final Logger LOGGER = Logger.get(LambdaCalculator.class);

    private static Set<Pattern> filterPatterns(Collection<Pattern> counted,
                                               Set<Pattern> patterns) {
        Set<Pattern> result = new HashSet<>();
        for (Pattern contPattern : patterns) {
            if (contPattern.get(contPattern.size() - 1) != WSKP
                    || contPattern.size() == 1
                    || !getHistPattern(contPattern).containsOnly(CNT)
                    || !counted.contains(contPattern)
                    || !counted.contains(getAbsPattern(contPattern))
                    || (contPattern.size() != 2 && !counted.contains(getHistLambdaPattern(contPattern))))
                continue;

            result.add(contPattern);
        }
        return result;
    }

    private static Pattern getAbsPattern(Pattern contPattern) {
        return contPattern.set(contPattern.size() - 1, SKP);
    }

    private static Pattern getHistPattern(Pattern contPattern) {
        return contPattern.range(0, contPattern.size() - 1);
    }

    private static Pattern getHistLambdaPattern(Pattern contPattern) {
        return contPattern.range(0, contPattern.size() - 2).concat(WSKP);
    }

    private class Thread implements Callable<Object> {
        private Pattern contPattern;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                contPattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (contPattern == null)
                    continue;

                LOGGER.debug("Calculating pattern '%s'.", contPattern);

                calculateLambdasForPattern();

                status.addModelLambda(Constants.MODEL_MODKNESERNEY_NAME,
                        contPattern);

                LOGGER.debug("Finished pattern '%s'.", contPattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void calculateLambdasForPattern() throws Exception {
            Pattern absPattern = getAbsPattern(contPattern);
            Pattern histPattern = getHistPattern(contPattern);
            Pattern histLambdaPattern = getHistLambdaPattern(contPattern);

            LOGGER.trace("contPattern       : %s", contPattern);
            LOGGER.trace("absPattern        : %s", absPattern);
            LOGGER.trace("histPattern       : %s", histPattern);
            LOGGER.trace("histLambdaPattern : %s", histLambdaPattern);

            Path contCountFile = continuationDir.resolve(contPattern.toString());
            Path absCountFile = absoluteDir.resolve(absPattern.toString());
            Path histCountFile = absoluteDir.resolve(histPattern.toString());
            Path histLambdaFile = lambdaDir.resolve(histLambdaPattern.toString());

            Path lambdaFile = lambdaDir.resolve(contPattern.toString());

            boolean checkHistLambda = histLambdaPattern.size() > 1;

            Discount discount = discounts.get(histPattern);

            try (CountsReader contReader = new CountsReader(contCountFile,
                    Constants.CHARSET, readerMemory / 4);
                    CountsReader absReader = new CountsReader(absCountFile,
                            Constants.CHARSET, readerMemory / 4);
                    CountsReader histReader = new CountsReader(histCountFile,
                            Constants.CHARSET, readerMemory / 4);
                    LambdaCountsReader histLambdaReader = !checkHistLambda
                            ? null
                            : new LambdaCountsReader(histLambdaFile,
                                    Constants.CHARSET, readerMemory / 4);
                    LambdaCountsWriter writer = new LambdaCountsWriter(
                            lambdaFile, Constants.CHARSET, writerMemory)) {
                while (contReader.readLine() != null) {
                    String contSequence = contReader.getSequence();
                    List<String> split = StringUtils.splitAtChar(contSequence,
                            ' ');

                    String histSequence = StringUtils.join(split.subList(0,
                            split.size() - 1), ' ');
                    String absSequence = histSequence + " " + SKP_WORD;

                    absReader.forwardToSequence(absSequence);
                    histReader.forwardToSequence(histSequence);

                    Counts cont = contReader.getCounts();

                    long absDen = absReader.getCount();
                    long contDen = contReader.getCount();

                    double gammaNum = discount.getOne() * cont.getOneCount()
                            + discount.getTwo() * cont.getTwoCount()
                            + discount.getThree() * cont.getThreePlusCount();
                    double gammaHigh = gammaNum / absDen;
                    double gammaLow = gammaNum / contDen;

                    LOGGER.trace("contSequence : %s", contSequence);
                    LOGGER.trace("absSequence  : %s", absSequence);
                    LOGGER.trace("histSequence : %s", histSequence);

                    LambdaCounts histLambdas = new LambdaCounts();
                    LambdaCount histLambda = new LambdaCount(1.0, 1.0);
                    if (checkHistLambda) {
                        String histLambdaSequence = StringUtils.join(
                                split.subList(0, split.size() - 2), ' ')
                                + " " + WSKP_WORD;
                        LOGGER.trace("histLambdaSequence : %s",
                                histLambdaSequence);
                        histLambdaReader.forwardToSequence(histLambdaSequence);
                        histLambdas = histLambdaReader.getLambdaCounts();
                        histLambda = histLambdas.get(0);
                    }

                    LambdaCount lambda = new LambdaCount(gammaHigh
                            * histLambda.getLow(), gammaLow
                            * histLambda.getLow());
                    LambdaCounts lambdas = new LambdaCounts();
                    lambdas.append(lambda);
                    for (LambdaCount l : histLambdas)
                        lambdas.append(l);

                    writer.append(contSequence, lambdas);
                }
            }
        }
    }

    private Config config;

    private Progress progress;
    private Status status;
    private Path absoluteDir;
    private Path continuationDir;
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
                                 Path absoluteDir,
                                 Path continuationDir,
                                 Path lambdaDir,
                                 Path discountsFile) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_LAMBDAS);

        LOGGER.debug("patterns = %s", patterns);

        LOGGER.debug("Filtering patterns.");
        patterns = filterPatterns(status.getCounted(), patterns);
        LOGGER.debug("Remaining patterns = %s", patterns);

        if (patterns.isEmpty())
            return;

        Files.createDirectories(lambdaDir);

        this.status = status;
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        this.lambdaDir = lambdaDir;
        this.discountsFile = discountsFile;
        patternQueue = new PriorityBlockingQueue<>(patterns);
        calculateMemory();
        loadDiscounts();

        progress = OUTPUT.newProgress(patternQueue.size());
        // Can't really parallelize MKN lambda calculation.
        ThreadUtils.executeThreads(1, Arrays.asList(new Thread()));
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
