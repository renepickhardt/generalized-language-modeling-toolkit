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

package de.glmtk.querying.probability;

import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.WSKP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.AbstractWorkerExecutor;
import de.glmtk.common.Config;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Patterns;
import de.glmtk.common.Status;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;


public class QueryCacherCreator extends AbstractWorkerExecutor<Pattern> {
    private static final Logger LOGGER = Logger.get(QueryCacherCreator.class);
    private static final String PHASE_SCANNING_COUNTS = "Scanning Counts";

    protected class Worker extends AbstractWorkerExecutor<Pattern>.Worker {
        protected Path patternFile;
        protected Path targetPatternFile;
        protected String leadingSequence;
        protected String trailingSequence;
        protected Queue<String> neededSequences;
        protected String nextSequence;

        @Override
        protected void work(Pattern pattern,
                            int patternNo) throws IOException {
            extractSequences(pattern);
            // LOGGER.trace("neededSequences = %s", neededSequences);
            getPatternFiles(pattern);
            filterAndWriteSequenceCounts();

            status.addQueryCacheCounted(name, pattern);
        }

        protected void extractSequences(Pattern pattern) throws IOException {
            Pattern corePattern = calculateCorePattern(pattern);
            if (corePattern.isEmpty()) {
                neededSequences = new ArrayDeque<>();
                neededSequences.add(trailingSequence.substring(1));
                return;
            }

            Set<String> sequences = new HashSet<>();
            try (BufferedReader reader = NioUtils.newBufferedReader(queryFile,
                Constants.CHARSET, readerMemory)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    List<String> sequenceList = StringUtils.split(line, ' ');
                    String[] split =
                        sequenceList.toArray(new String[sequenceList.size()]);
                    String[] words = new String[split.length];
                    String[] poses = new String[split.length];
                    StringUtils.extractWordsAndPoses(split, queryFileTagged,
                        words, poses);

                    int patternSize = corePattern.size();
                    for (int p = 0; p <= words.length - patternSize; ++p) {
                        String sequence = corePattern.apply(words, poses, p);
                        sequences
                            .add(leadingSequence + sequence + trailingSequence);
                    }
                }
            }

            neededSequences = new ArrayDeque<>(new TreeSet<>(sequences));
        }

        protected Pattern calculateCorePattern(Pattern pattern) {
            LOGGER.trace("Calculating Core Pattern:");

            int cntLeading = 0;
            StringBuilder leadingSequenceBuilder = new StringBuilder();
            for (int i = 0; i != pattern.size(); ++i) {
                PatternElem elem = pattern.get(i);
                if (!elem.equals(SKP) && !elem.equals(WSKP)) {
                    break;
                }

                ++cntLeading;
                leadingSequenceBuilder.append(elem.apply(null)).append(' ');
            }

            StringBuilder trailingSequenceBuilder = new StringBuilder();
            int cntTrailing = 0;
            for (int i = pattern.size() - 1; i != -1; --i) {
                PatternElem elem = pattern.get(i);
                if (!elem.equals(SKP) && !elem.equals(WSKP)) {
                    break;
                }

                ++cntTrailing;
                trailingSequenceBuilder.append(elem.apply(null)).append(' ');
            }

            leadingSequence = leadingSequenceBuilder.toString();
            trailingSequence = trailingSequenceBuilder.reverse().toString();

            LOGGER.trace("Pattern:        %s", pattern);
            LOGGER.trace("cntLeading:     %d", cntLeading);
            LOGGER.trace("cntTrailing:    %d", cntTrailing);
            LOGGER.trace("stringLeading:  '%s'", leadingSequence);
            LOGGER.trace("stringTrailing: '%s'", trailingSequence);

            if (cntLeading >= pattern.size() - cntTrailing) {
                return Patterns.get();
            }

            Pattern corePattern =
                pattern.range(cntLeading, pattern.size() - cntTrailing);

            LOGGER.trace("Core Pattern:  %s", corePattern);

            return corePattern;
        }

        private void getPatternFiles(Pattern pattern) {
            if (pattern.isAbsolute()) {
                patternFile = absoluteDir.resolve(pattern.toString());
                targetPatternFile =
                    targetAbsoluteDir.resolve(pattern.toString());
            } else {
                patternFile = continuationDir.resolve(pattern.toString());
                targetPatternFile =
                    targetContinuationDir.resolve(pattern.toString());
            }
        }

        protected void filterAndWriteSequenceCounts() throws IOException {
            try (CountsReader reader =
                new CountsReader(patternFile, Constants.CHARSET, readerMemory);
                 BufferedWriter writer = NioUtils.newBufferedWriter(
                     targetPatternFile, Constants.CHARSET, writerMemory)) {
                nextSequence = neededSequences.poll();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (nextSequence == null) {
                        break;
                    }

                    String sequence = reader.getSequence();
                    if (sequence == null) {
                        continue;
                    }

                    if (isSequenceNeeded(sequence)) {
                        writer.append(line).append('\n');
                    }
                }
            }
        }

        protected boolean isSequenceNeeded(String sequence) {
            if (nextSequence == null) {
                return false;
            }

            int cmp;
            while ((cmp = sequence.compareTo(nextSequence)) >= 0) {
                if (cmp == 0) {
                    return true;
                }

                nextSequence = neededSequences.poll();
                if (nextSequence == null) {
                    return false;
                }
            }

            return false;
        }
    }

    private Path absoluteDir;
    private Path continuationDir;
    private Path targetAbsoluteDir;
    private Path targetContinuationDir;
    private String name;
    protected Path queryFile;
    protected boolean queryFileTagged;
    private Status status;

    public QueryCacherCreator(Config config) {
        super(config);
    }

    public GlmtkPaths createQueryCache(String name,
                                       Path queryFile,
                                       boolean queryFileTagged,
                                       Set<Pattern> patterns,
                                       Status status,
                                       GlmtkPaths paths) throws Exception {
        ProgressBar progressBar = new ProgressBar(PHASE_SCANNING_COUNTS);

        GlmtkPaths queryCachePaths = paths.newQueryCache(name);
        queryCachePaths.logPaths();

        absoluteDir = paths.getAbsoluteDir();
        continuationDir = paths.getContinuationDir();
        targetAbsoluteDir = queryCachePaths.getAbsoluteDir();
        targetContinuationDir = queryCachePaths.getContinuationDir();
        this.name = name;
        this.queryFile = queryFile;
        this.queryFileTagged = queryFileTagged;
        this.status = status;

        Files.createDirectories(targetAbsoluteDir);
        Files.createDirectories(targetContinuationDir);

        work(patterns, progressBar);

        return queryCachePaths;
    }

    @Override
    protected Collection<? extends Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i) {
            workers.add(new Worker());
        }
        return workers;
    }
}
