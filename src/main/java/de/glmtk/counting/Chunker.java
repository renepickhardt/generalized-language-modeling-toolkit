/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
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

package de.glmtk.counting;

import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.counts.Counts;
import de.glmtk.files.CountsReader;
import de.glmtk.files.CountsWriter;
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;


public class Chunker {
    private static final Logger LOGGER = Logger.get(Chunker.class);
    private static final int TAB_COUNTS_NL_BYTES =
        ("\t" + new Counts(10, 10, 10, 10).toString() + "\n")
            .getBytes(Constants.CHARSET).length;

    private abstract class Thread implements Callable<Object> {
        protected Pattern pattern;
        private Path patternDir;
        private Set<String> chunkFiles;
        protected long chunkSize;
        protected Map<String, Counts> chunkCounts;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                pattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                    TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    continue;
                }

                List<Path> inputFiles = getInputFiles();
                if (inputFiles == null) {
                    LOGGER.trace("Pattern '%s' not vailable.", pattern);
                    StatisticalNumberHelper.count("Pattern not available");
                    // wait until other threads finishes pattern
                    java.lang.Thread.sleep(10);
                    patternQueue.put(pattern);
                    continue;
                }

                LOGGER.debug("Chunking pattern '%s'.", pattern);

                if (absolute) {
                    patternDir = absoluteChunkedDir.resolve(pattern.toString());
                } else {
                    patternDir =
                        continuationChunkedDir.resolve(pattern.toString());
                }
                chunkFiles = new LinkedHashSet<>();
                chunkSize = 0L;
                chunkCounts = new HashMap<>();

                Files.createDirectories(patternDir);

                for (Path inputFile : inputFiles) {
                    sequenceInput(inputFile);
                }

                writeChunkToFile(); // Write remaining partial chunk.
                chunkCounts = null; // Free memory of map.

                status.setChunksForPattern(pattern, chunkFiles);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progressBar) {
                    progressBar.increase();
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        protected abstract List<Path> getInputFiles() throws Exception;

        protected abstract void sequenceInput(Path inputFile) throws Exception;

        protected void writeChunkToFile() throws IOException {
            Path chunkFile = patternDir.resolve("chunk" + chunkFiles.size());
            Files.deleteIfExists(chunkFile);

            Map<String, Counts> sortedCounts = new TreeMap<>(chunkCounts);
            try (CountsWriter writer =
                new CountsWriter(chunkFile, Constants.CHARSET, writerMemory)) {
                for (Entry<String, Counts> entry : sortedCounts.entrySet()) {
                    String sequence = entry.getKey();
                    Counts counts = entry.getValue();
                    if (absolute) {
                        writer.append(sequence, counts.getOnePlusCount());
                    } else {
                        writer.append(sequence, counts);
                    }
                }
            }

            chunkFiles.add(chunkFile.getFileName().toString());

            LOGGER.debug("Wrote chunk for pattern '%s': '%s'.", pattern,
                chunkFile);
        }
    }

    private class AbsoluteThread extends Thread {
        @Override
        protected List<Path> getInputFiles() throws Exception {
            return Arrays.asList(trainingFile);
        }

        @Override
        protected void sequenceInput(Path inputFile) throws Exception {
            int patternSize = pattern.size();
            if (trainingCache != null) {
                for (String line : trainingCache) {
                    perLine(line, patternSize);
                }
            } else {
                int memory =
                    (int) Math.min(Files.size(inputFile), readerMemory);
                try (BufferedReader reader = NioUtils
                    .newBufferedReader(inputFile, Constants.CHARSET, memory)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        perLine(line, patternSize);
                    }
                }
            }
        }

        private void perLine(String line,
                             int patternSize) throws IOException {
            String[] split =
                StringUtils.split(line, ' ').toArray(new String[0]);
            String[] words = new String[split.length];
            String[] poses = new String[split.length];
            StringUtils.extractWordsAndPoses(split, trainingFileTagged, words,
                poses);

            for (int p = 0; p <= split.length - patternSize; ++p) {
                String sequence = pattern.apply(words, poses, p);
                countSequence(sequence);
            }
        }

        private void countSequence(String sequence) throws IOException {
            Counts counts = chunkCounts.get(sequence);
            if (counts == null) {
                counts = new Counts();
                chunkCounts.put(sequence, counts);
                chunkSize += sequence.getBytes(Constants.CHARSET).length
                    + TAB_COUNTS_NL_BYTES;
            }
            counts.add(1L);

            if (chunkSize > maxChunkSize) {
                if (Constants.DEBUG_AVERAGE_MEMORY) {
                    StatisticalNumberHelper.average("Chunk Map Memory",
                        MemoryUtil.deepMemoryUsageOf(chunkCounts,
                            VisibilityFilter.ALL));
                }

                writeChunkToFile();
                chunkSize = 0L;
                chunkCounts = new HashMap<>();
            }
        }
    }

    private class ContinuationThread extends Thread {
        @Override
        protected List<Path> getInputFiles() throws IOException {
            Pattern inputPattern = pattern.getContinuationSource();

            Path inputDir;
            boolean fromChunked;

            boolean isAbsolute = inputPattern.isAbsolute();
            if (isAbsolute && status.getCounted(true).contains(inputPattern)) {
                inputDir = absoluteDir;
                fromChunked = false;
            } else if (isAbsolute
                && status.getChunkedPatterns(true).contains(inputPattern)) {
                inputDir = absoluteChunkedDir;
                fromChunked = true;
            } else if (!isAbsolute
                && status.getCounted(false).contains(inputPattern)) {
                inputDir = continuationDir;
                fromChunked = false;
            } else if (!isAbsolute
                && status.getChunkedPatterns(false).contains(inputPattern)) {
                inputDir = continuationChunkedDir;
                fromChunked = true;
            } else {
                return null;
            }

            inputDir = inputDir.resolve(inputPattern.toString());

            if (!fromChunked) {
                return Arrays.asList(inputDir);
            }

            List<Path> result = new ArrayList<>();
            try (DirectoryStream<Path> inputDirStream =
                Files.newDirectoryStream(inputDir)) {
                for (Path inputFile : inputDirStream) {
                    result.add(inputFile);
                }
            }
            return result;
        }

        @Override
        protected void sequenceInput(Path inputFile) throws Exception {
            LOGGER.debug("Sequencing '%s' from '%s'.", pattern, inputFile);
            int memory = (int) Math.min(Files.size(inputFile), readerMemory);
            try (CountsReader reader =
                new CountsReader(inputFile, Constants.CHARSET, memory)) {
                while (reader.readLine() != null) {
                    String sequence = reader.getSequence();
                    Counts counts = reader.getCounts();
                    boolean fromAbsolute = reader.isFromAbsolute();

                    String appliedSequence = pattern.apply(StringUtils
                        .split(sequence, ' ').toArray(new String[0]));
                    countSequence(appliedSequence, counts, fromAbsolute);
                }
            }
        }

        private void countSequence(String sequence,
                                   Counts sequenceCounts,
                                   boolean fromAbsolute) throws IOException {
            Counts counts = chunkCounts.get(sequence);
            if (counts == null) {
                counts = new Counts();
                chunkCounts.put(sequence, counts);
                chunkSize += sequence.getBytes(Constants.CHARSET).length
                    + TAB_COUNTS_NL_BYTES;
            }
            if (fromAbsolute) {
                counts.addOne(sequenceCounts.getOnePlusCount());
            } else {
                counts.add(sequenceCounts);
            }

            if (chunkSize > maxChunkSize) {
                if (Constants.DEBUG_AVERAGE_MEMORY) {
                    StatisticalNumberHelper.average("Chunk Map Memory",
                        MemoryUtil.deepMemoryUsageOf(chunkCounts,
                            VisibilityFilter.ALL));
                }

                writeChunkToFile();
                chunkSize = 0L;
                chunkCounts = new HashMap<>();
            }
        }
    }

    private Config config;

    private ProgressBar progressBar;
    private boolean absolute;
    private Status status;
    private Path trainingFile;
    private boolean trainingFileTagged;
    private Path absoluteDir;
    private Path continuationDir;
    private Path absoluteChunkedDir;
    private Path continuationChunkedDir;
    private BlockingQueue<Pattern> patternQueue;
    private List<String> trainingCache;
    private int readerMemory;
    private int writerMemory;
    private long maxChunkSize;

    public Chunker(Config config) {
        this.config = config;
    }

    public void chunkAbsolute(Status status,
                              Set<Pattern> patterns,
                              Path trainingFile,
                              boolean trainingFileTagged,
                              Path absoluteChunkedDir,
                              ProgressBar progressBar) throws Exception {
        this.status = status;
        this.trainingFile = trainingFile;
        this.trainingFileTagged = trainingFileTagged;
        this.absoluteChunkedDir = absoluteChunkedDir;
        chunk(true, patterns, progressBar);
    }

    public void chunkContinuation(Status status,
                                  Set<Pattern> patterns,
                                  Path absoluteDir,
                                  Path continuationDir,
                                  Path absoluteChunkedDir,
                                  Path continuationChunkedDir,
                                  ProgressBar progressBar) throws Exception {
        this.status = status;
        this.absoluteDir = absoluteDir;
        this.absoluteChunkedDir = absoluteChunkedDir;
        this.continuationDir = continuationDir;
        this.continuationChunkedDir = continuationChunkedDir;
        chunk(false, patterns, progressBar);
    }

    private void chunk(boolean absolute,
                       Set<Pattern> patterns,
                       ProgressBar progressBar) throws Exception {
        LOGGER.debug("patterns = '%s'", patterns);
        if (patterns.isEmpty()) {
            return;
        }

        this.absolute = absolute;
        patternQueue = new PriorityBlockingQueue<>(patterns);
        calculateMemory();

        if (!absolute
            || Files.size(trainingFile) > config.getMemoryCacheThreshold()) {
            trainingCache = null;
        } else {
            int memory = (int) Math.min(Files.size(trainingFile), readerMemory);
            try (BufferedReader reader = NioUtils
                .newBufferedReader(trainingFile, Constants.CHARSET, memory)) {
                trainingCache = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    trainingCache.add(line);
                }
            }
        }

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i) {
            if (absolute) {
                threads.add(new AbsoluteThread());
            } else {
                threads.add(new ContinuationThread());
            }
        }

        this.progressBar = progressBar;
        this.progressBar.total(patternQueue.size());
        ThreadUtils.executeThreads(config.getNumberOfThreads(), threads);
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();
        maxChunkSize = config.getMemoryChunkSize();

        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
        LOGGER.debug("maxChunkSize = %s", humanReadableByteCount(maxChunkSize));
    }
}
