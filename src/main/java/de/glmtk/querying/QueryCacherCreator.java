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
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.common.Status;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public class QueryCacherCreator {
    private static final Logger LOGGER = Logger.get(QueryCacherCreator.class);

    private class Thread implements Callable<Object> {
        private Pattern pattern;
        private Path patternFile;
        private Path targetPatternFile;
        private Queue<String> neededSequences;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                pattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (pattern == null)
                    continue;

                LOGGER.debug("Caching pattern '%s'.", pattern);

                extractSequences();
                getPatternFiles();
                filterAndWriteSequenceCounts();

                status.addQueryCacheCounted(name, pattern);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void extractSequences() throws IOException {
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

        private void getPatternFiles() {
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

    private Config config;

    private Progress progress;
    private Status status;
    private String name;
    private Path queryFile;
    private boolean queryFileTagged;
    private Path absoluteDir;
    private Path continuationDir;
    private Path targetAbsoluteDir;
    private Path targetContinuationDir;
    private BlockingQueue<Pattern> patternQueue;
    private int readerMemory;
    private int writerMemory;

    public QueryCacherCreator(Config config) {
        this.config = config;
    }

    public void createQueryCache(Status status,
                                 Set<Pattern> patterns,
                                 String name,
                                 Path queryFile,
                                 boolean queryFileTagged,
                                 Path absoluteDir,
                                 Path continuationDir,
                                 Path targetAbsoluteDir,
                                 Path targetContinuationDir) throws Exception {
        OUTPUT.setPhase(Phase.SCANNING_COUNTS);

        LOGGER.debug("patterns = '%s'", patterns);
        if (patterns.isEmpty())
            return;

        Files.createDirectories(targetAbsoluteDir);
        Files.createDirectories(targetContinuationDir);

        this.status = status;
        this.name = name;
        this.queryFile = queryFile;
        this.queryFileTagged = queryFileTagged;
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        this.targetAbsoluteDir = targetAbsoluteDir;
        this.targetContinuationDir = targetContinuationDir;
        patternQueue = new LinkedBlockingQueue<>();
        patternQueue.addAll(patterns);
        calculateMemory();

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
}
