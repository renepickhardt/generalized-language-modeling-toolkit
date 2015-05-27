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

import static com.google.common.base.Throwables.getStackTraceAsString;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

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
import de.glmtk.util.ThreadUtils;

public class Merger {
    private static final Logger LOGGER = Logger.get(Merger.class);

    private class Thread implements Callable<Object> {
        @Override
        public Object call() throws InterruptedException, IOException {
            while (!patternQueue.isEmpty()) {
                Pattern pattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    LOGGER.trace("Thread Idle.");
                    StatisticalNumberHelper.count("Merger#Thread idle");
                    continue;
                }

                mergePattern(pattern);

                synchronized (progressBar) {
                    progressBar.increase();
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void mergePattern(Pattern pattern) throws IOException {
            Path patternDir = chunkedDir.resolve(pattern.toString());

            Set<String> chunksForPattern;
            while ((chunksForPattern = status.getChunksForPattern(pattern)).size() != 1) {
                int numParallelChunks = Math.min(numParallelReaders,
                        chunksForPattern.size());

                Set<String> chunksToMerge = new LinkedHashSet<>();
                Iterator<String> iter = chunksForPattern.iterator();
                for (int i = 0; i != numParallelChunks; ++i)
                    chunksToMerge.add(iter.next());

                Path mergeFile = patternDir.resolve("merge"
                        + getMergeCounter(chunksForPattern));
                LOGGER.debug("Merging pattern %s:\t%s -> %s.", pattern,
                        chunksToMerge, mergeFile.getFileName());
                mergeChunksToFile(patternDir, chunksToMerge, mergeFile);

                synchronized (status) {
                    try {
                        status.performMergeForChunks(pattern, chunksToMerge,
                                mergeFile.getFileName().toString());
                    } catch (IOException e) {
                        // Updating status did not work, we continue in the hope
                        // it works next time.
                        LOGGER.warn("Unable to write status, continuing: "
                                + getStackTraceAsString(e));
                    }

                    for (String chunk : chunksToMerge)
                        Files.deleteIfExists(patternDir.resolve(chunk));
                }
            }

            Path src = patternDir.resolve(chunksForPattern.iterator().next());
            Path dest = countedDir.resolve(pattern.toString());

            synchronized (status) {
                LOGGER.debug("Finishing pattern %s:\t%s\t -> %s.", pattern,
                        src, dest);
                Files.deleteIfExists(dest);
                Files.move(src, dest);
                status.finishMerge(pattern);

                if (NioUtils.isDirEmpty(patternDir))
                    Files.deleteIfExists(patternDir);
            }
        }

        private int getMergeCounter(Set<String> chunksForPattern) {
            int maxMergeNr = 0;
            for (String chunk : chunksForPattern)
                if (chunk.startsWith("merge")) {
                    int mergeNr = Integer.parseInt(chunk.substring("merge".length()));
                    if (maxMergeNr < mergeNr)
                        maxMergeNr = mergeNr;
                }
            return maxMergeNr + 1;
        }

        private void mergeChunksToFile(Path patternDir,
                                       Set<String> chunksToMerge,
                                       Path mergeFile) throws IOException {
            Files.deleteIfExists(mergeFile);

            PriorityQueue<CountsReader> readerQueue = new PriorityQueue<>(
                    chunksToMerge.size(), CountsReader.SEQUENCE_COMPARATOR);

            int memoryPerReader = (int) (readerMemory / chunksToMerge.size());
            for (String chunk : chunksToMerge) {
                Path chunkFile = patternDir.resolve(chunk);
                int memory = (int) Math.min(Files.size(chunkFile),
                        memoryPerReader);

                // Reader is closed later manually so we supress the warning.
                @SuppressWarnings("resource")
                CountsReader reader = new CountsReader(chunkFile,
                        Constants.CHARSET, memory);

                reader.readLine();
                readerQueue.add(reader);
            }

            try (CountsWriter writer = new CountsWriter(mergeFile,
                    Constants.CHARSET, (int) writerMemory)) {
                String lastSequence = null;
                Counts countsAgg = null;
                while (!readerQueue.isEmpty()) {
                    @SuppressWarnings("resource")
                    CountsReader reader = readerQueue.poll();
                    if (reader == null)
                        continue;
                    if (reader.isEof()) {
                        reader.close();
                        continue;
                    }

                    String sequence = reader.getSequence();
                    Counts counts = reader.getCounts();

                    if (sequence.equals(lastSequence))
                        countsAgg.add(counts);
                    else {
                        if (lastSequence != null)
                            if (absolute)
                                writer.append(lastSequence,
                                        countsAgg.getOnePlusCount());
                            else
                                writer.append(lastSequence, countsAgg);
                        lastSequence = sequence;
                        countsAgg = counts;
                    }

                    reader.readLine();
                    readerQueue.add(reader);
                }

                if (lastSequence != null)
                    if (absolute)
                        writer.append(lastSequence, countsAgg.getOnePlusCount());
                    else
                        writer.append(lastSequence, countsAgg);
            } finally {
                for (CountsReader reader : readerQueue)
                    reader.close();
            }
        }
    }

    private Config config;

    private int numParallelReaders = 10;
    private ProgressBar progressBar;
    private boolean absolute;
    private Status status;
    private Path chunkedDir;
    private Path countedDir;
    private BlockingQueue<Pattern> patternQueue;
    private long readerMemory;
    private long writerMemory;

    public Merger(Config config) {
        this.config = config;
    }

    public void mergeAbsolute(Status status,
                              Set<Pattern> patterns,
                              Path chunkedDir,
                              Path countedDir,
                              ProgressBar progressBar) throws Exception {
        merge(true, status, patterns, chunkedDir, countedDir, progressBar);
    }

    public void mergeContinuation(Status status,
                                  Set<Pattern> patterns,
                                  Path chunkedDir,
                                  Path countedDir,
                                  ProgressBar progessBar) throws Exception {
        merge(false, status, patterns, chunkedDir, countedDir, progessBar);
    }

    private void merge(boolean absolute,
                       Status status,
                       Set<Pattern> patterns,
                       Path chunkedDir,
                       Path countedDir,
                       ProgressBar progressBar) throws Exception {
        LOGGER.debug("patterns = %s", patterns);
        if (patterns.isEmpty())
            return;

        Files.createDirectories(countedDir);

        this.absolute = absolute;
        this.status = status;
        this.chunkedDir = chunkedDir;
        this.countedDir = countedDir;
        patternQueue = new LinkedBlockingDeque<>(patterns);
        calculateMemory();

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        this.progressBar = progressBar;
        this.progressBar.total(patternQueue.size());
        ThreadUtils.executeThreads(config.getNumberOfThreads(), threads);

        if (NioUtils.isDirEmpty(chunkedDir))
            Files.deleteIfExists(chunkedDir);
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();

        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }
}
