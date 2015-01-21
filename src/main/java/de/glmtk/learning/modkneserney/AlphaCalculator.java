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

import static de.glmtk.Constants.MODEL_MODKNESERNEY;
import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.SKP_WORD;
import static de.glmtk.common.PatternElem.WSKP;
import static de.glmtk.common.PatternElem.WSKP_WORD;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.common.AbstractWorkerExecutor;
import de.glmtk.common.Cache;
import de.glmtk.common.CacheBuilder;
import de.glmtk.common.Config;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.AlphaCount;
import de.glmtk.counts.Discount;
import de.glmtk.files.AlphaCountWriter;
import de.glmtk.files.CountsReader;
import de.glmtk.util.StringUtils;

public class AlphaCalculator extends AbstractWorkerExecutor<Pattern> {
    private static Set<Pattern> filterPatterns(Collection<Pattern> counted,
                                               Collection<Pattern> patterns) {
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

    private class Worker extends AbstractWorkerExecutor<Pattern>.Worker {
        @Override
        protected void work(Pattern numPattern) throws Exception {
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
                discount = cache.getDiscount(MODEL_MODKNESERNEY, histPattern);

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

            status.addAlpha(MODEL_MODKNESERNEY, numPattern);
        }
    }

    private Path absoluteDir;
    private Path continuationDir;
    private Path alphaDir;
    private Cache cache;

    public AlphaCalculator(Config config) {
        super(config);
    }

    @Override
    protected Collection<Pattern> prepare(Collection<Pattern> patterns) throws Exception {
        absoluteDir = paths.getAbsoluteDir();
        continuationDir = paths.getContinuationDir();
        alphaDir = paths.getModKneserNeyAlphaDir();

        patterns = filterPatterns(status.getCounted(), patterns);
        patterns.removeAll(status.getAlphas(MODEL_MODKNESERNEY));

        cache = new CacheBuilder(paths).withDiscounts(MODEL_MODKNESERNEY).build();

        Files.createDirectories(alphaDir);

        return patterns;
    }

    @Override
    protected Collection<Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            workers.add(new Worker());
        return workers;
    }

}
