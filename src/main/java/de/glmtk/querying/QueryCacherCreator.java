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

package de.glmtk.querying;

import static de.glmtk.common.Output.OUTPUT;

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
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Status;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

public class QueryCacherCreator extends AbstractWorkerExecutor<Pattern> {
    private static final Logger LOGGER = Logger.get(QueryCacherCreator.class);

    private class Worker extends AbstractWorkerExecutor<Pattern>.Worker {
        private Path patternFile;
        private Path targetPatternFile;
        private Queue<String> neededSequences;

        @Override
        protected void work(Pattern pattern,
                            int patternNo) throws IOException {
            extractSequences(pattern);
            getPatternFiles(pattern);
            filterAndWriteSequenceCounts();

            status.addQueryCacheCounted(name, pattern);
        }

        private void extractSequences(Pattern pattern) throws IOException {
            Set<String> sequences = new HashSet<>();

            LOGGER.trace("Calculating Core Pattern:");

            int cntLeading = 0;
            StringBuilder stringLeadingBuilder = new StringBuilder();
            for (int i = 0; i != pattern.size(); ++i)
                if (pattern.get(i).equals(PatternElem.SKP)) {
                    ++cntLeading;
                    stringLeadingBuilder.append(PatternElem.SKP_WORD).append(
                            ' ');
                } else if (pattern.get(i).equals(PatternElem.WSKP)) {
                    ++cntLeading;
                    stringLeadingBuilder.append(PatternElem.WSKP_WORD).append(
                            ' ');
                } else
                    break;

            StringBuilder stringTrailingBuilder = new StringBuilder();
            int cntTrailing = 0;
            for (int i = pattern.size() - 1; i != -1; --i)
                if (pattern.get(i).equals(PatternElem.SKP)) {
                    ++cntTrailing;
                    stringTrailingBuilder.append(PatternElem.SKP_WORD).append(
                            ' ');
                } else if (pattern.get(i).equals(PatternElem.WSKP)) {
                    ++cntTrailing;
                    stringTrailingBuilder.append(PatternElem.WSKP_WORD).append(
                            ' ');
                } else
                    break;

            String stringLeading = stringLeadingBuilder.toString();
            String stringTrailing = stringTrailingBuilder.reverse().toString();

            LOGGER.trace("Pattern:        %s", pattern);
            LOGGER.trace("cntLeading:     %d", cntLeading);
            LOGGER.trace("cntTrailing:    %d", cntTrailing);
            LOGGER.trace("stringLeading:  '%s'", stringLeading);
            LOGGER.trace("stringTrailing: '%s'", stringTrailing);

            if (cntTrailing == pattern.size()) {
                neededSequences = new ArrayDeque<>();
                neededSequences.add(stringTrailing.substring(1));
                return;
            }

            Pattern corePattern = pattern.range(cntLeading, pattern.size()
                    - cntTrailing);

            LOGGER.trace("Core Pattern:  %s", corePattern);

            int patternSize = corePattern.size();
            try (BufferedReader reader = NioUtils.newBufferedReader(queryFile,
                    Constants.CHARSET, readerMemory)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split = StringUtils.split(line, ' ').toArray(
                            new String[0]);
                    String[] words = new String[split.length];
                    String[] poses = new String[split.length];
                    StringUtils.extractWordsAndPoses(split, queryFileTagged,
                            words, poses);

                    for (int p = 0; p <= split.length - patternSize; ++p) {
                        String sequence = corePattern.apply(words, poses, p);
                        sequences.add(stringLeading + sequence + stringTrailing);
                    }
                }
            }

            neededSequences = new ArrayDeque<>();
            neededSequences.addAll(new TreeSet<>(sequences));
        }

        private void getPatternFiles(Pattern pattern) {
            if (pattern.isAbsolute()) {
                patternFile = absoluteDir.resolve(pattern.toString());
                targetPatternFile = targetAbsoluteDir.resolve(pattern.toString());
            } else {
                patternFile = continuationDir.resolve(pattern.toString());
                targetPatternFile = targetContinuationDir.resolve(pattern.toString());
            }
        }

        private void filterAndWriteSequenceCounts() throws IOException {
            try (CountsReader reader = new CountsReader(patternFile,
                    Constants.CHARSET, readerMemory);
                    BufferedWriter writer = NioUtils.newBufferedWriter(
                            targetPatternFile, Constants.CHARSET, writerMemory)) {
                String nextSequence = neededSequences.poll();
                if (nextSequence == null)
                    return;

                String line;
                while ((line = reader.readLine()) != null) {
                    String sequence = reader.getSequence();

                    int cmp;
                    while (nextSequence != null
                            && (cmp = sequence.compareTo(nextSequence)) >= 0) {
                        if (cmp == 0)
                            writer.append(line).append('\n');

                        nextSequence = neededSequences.poll();
                    }
                }
            }
        }
    }

    private Path absoluteDir;
    private Path continuationDir;
    private Path targetAbsoluteDir;
    private Path targetContinuationDir;
    private String name;
    private Path queryFile;
    private boolean queryFileTagged;
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
        OUTPUT.setPhase(Phase.SCANNING_COUNTS);

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

        work(patterns);

        return queryCachePaths;
    }

    @Override
    protected Collection<? extends Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            workers.add(new Worker());
        return workers;
    }
}
