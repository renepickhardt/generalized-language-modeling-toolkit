package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.common.Counter;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.util.NioUtils;
import de.glmtk.util.PrintUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public enum Merger {

    MERGER;

    private static class SequenceCountReader implements Closeable,
    AutoCloseable {

        public static final Comparator<SequenceCountReader> COMPARATOR =
                new Comparator<Merger.SequenceCountReader>() {

            @Override
            public int compare(
                    SequenceCountReader lhs,
                    SequenceCountReader rhs) {
                if (lhs == rhs) {
                    return 0;
                } else if (lhs == null) {
                    return 1;
                } else if (rhs == null) {
                    return -1;
                } else {
                    return StringUtils.compare(lhs.sequence,
                            rhs.sequence);
                }
            }

        };

        private Path path;

        private BufferedReader reader;

        private int lineNo;

        private String sequence;

        private Counter counter;

        public SequenceCountReader(
                Path path,
                Charset charset,
                int sz) throws IOException {
            this.path = path;
            reader = NioUtils.newBufferedReader(path, charset, sz);
            lineNo = -1;
            nextLine();
        }

        public void nextLine() throws IOException {
            String line = reader.readLine();
            ++lineNo;
            if (line == null) {
                sequence = null;
                counter = null;
            } else {
                counter = new Counter();
                try {
                    sequence = Counter.getSequenceAndCounter(line, counter);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(String.format(
                            "Illegal line '%d' in file '%s'.\n%s", lineNo,
                            path, e.getMessage()));
                }
            }
        }

        public String getSequence() {
            return sequence;
        }

        public Counter getCounter() {
            return counter;
        }

        public boolean isEmpty() {
            return sequence == null;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Merger.class);

    private static final int AVAILABLE_MEMORY_PERCENT = 35;

    private class Thread implements Callable<Object> {

        @Override
        public Object call() throws InterruptedException, IOException {
            while (!queue.isEmpty()) {
                Pattern pattern =
                        queue.poll(Constants.QUEUE_TIMEOUT,
                                TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    LOGGER.trace("Thread Idle.");
                    StatisticalNumberHelper.count("Merger#Thread idle");
                    continue;
                }

                mergePattern(pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void mergePattern(Pattern pattern) throws InterruptedException,
        IOException {
            Path patternDir = chunkedDir.resolve(pattern.toString());

            int mergeCounter = 0;
            List<Path> chunksForPattern, chunksToMerge = null;
            while ((chunksForPattern = status.getChunks(continuation, pattern))
                    .size() != 1) {
                int numParallelChunks =
                        Math.min(numParallelReaders, chunksForPattern.size());
                chunksToMerge =
                        new ArrayList<Path>(chunksForPattern.subList(0,
                                numParallelChunks));

                Path mergeFile = patternDir.resolve("merge" + mergeCounter);
                LOGGER.debug("Merging pattern %s:\t%s -> %s.", pattern,
                        chunksToMerge, mergeFile.getFileName());
                mergeChunksToFile(patternDir, chunksToMerge, mergeFile);
                try {
                    status.performChunkedMerge(continuation, pattern,
                            chunksToMerge, mergeFile.getFileName());
                } catch (IOException e) {
                    // Updating status did not work, we continue in the hope
                    // it works next time.
                }

                for (Path chunk : chunksToMerge) {
                    Files.delete(patternDir.resolve(chunk));
                }

                ++mergeCounter;
            }

            Path src = patternDir.resolve(chunksForPattern.get(0));
            Path dest = countedDir.resolve(pattern.toString());
            LOGGER.debug("Finishing pattern %s:\t%s\t -> %s.", pattern, src,
                    dest);
            Files.deleteIfExists(dest);
            Files.move(src, dest);
            status.finishChunkedMerge(continuation, pattern);

            if (NioUtils.isDirEmpty(patternDir)) {
                Files.delete(patternDir);
            }
        }

        private void mergeChunksToFile(
                Path patternDir,
                List<Path> chunksToMerge,
                Path mergeFile) throws IOException {
            Files.deleteIfExists(mergeFile);

            try (BufferedWriter writer =
                    NioUtils.newBufferedWriter(mergeFile, Constants.CHARSET,
                            (int) writerMemory)) {
                PriorityQueue<SequenceCountReader> readerQueue =
                        new PriorityQueue<SequenceCountReader>(
                                chunksToMerge.size(),
                                SequenceCountReader.COMPARATOR);
                int memoryPerReader =
                        (int) (readerMemory / chunksToMerge.size());
                for (Path chunk : chunksToMerge) {
                    readerQueue
                            .add(new SequenceCountReader(patternDir
                                    .resolve(chunk), Constants.CHARSET,
                                    memoryPerReader));
                }

                String lastSequence = null;
                Counter aggregationCounter = null;
                while (!readerQueue.isEmpty()) {
                    SequenceCountReader reader = readerQueue.poll();
                    if (reader.isEmpty()) {
                        reader.close();
                        continue;
                    }

                    String sequence = reader.getSequence();
                    Counter counter = reader.getCounter();

                    if (sequence.equals(lastSequence)) {
                        aggregationCounter.add(counter);
                    } else {
                        if (lastSequence != null) {
                            writer.write(lastSequence);
                            writer.write('\t');
                            if (continuation) {
                                writer.write(aggregationCounter.toString());
                            } else {
                                writer.write(Long.toString(aggregationCounter
                                        .getOnePlusCount()));
                            }
                            writer.write('\n');
                        }
                        lastSequence = sequence;
                        aggregationCounter = counter;
                    }
                    reader.nextLine();
                    readerQueue.add(reader);
                }
            }
        }
    }

    private boolean continuation;

    private int numParallelReaders = 10;

    private Status status;

    private long readerMemory;

    private long writerMemory;

    private BlockingQueue<Pattern> queue;

    private Path chunkedDir;

    private Path countedDir;

    private Progress progress;

    public void merge(
            Status status,
            boolean continuation,
            Set<Pattern> patterns,
            Path chunkedDir,
            Path countedDir) throws Exception {
        if (!continuation) {
            OUTPUT.setPhase(Phase.ABSOLUTE_MERGING, true);
        } else {
            OUTPUT.setPhase(Phase.CONTINUATION_MERGING, true);
        }
        if (patterns.isEmpty()) {
            LOGGER.debug("No chunks to merge, returning.");
            OUTPUT.setPercent(1.0);
            return;
        }

        LOGGER.debug("patterns = %s", patterns);
        Files.createDirectories(countedDir);

        calculateMemory();
        this.status = status;
        this.continuation = continuation;
        this.chunkedDir = chunkedDir;
        this.countedDir = countedDir;
        progress = new Progress(patterns.size());
        queue = new LinkedBlockingDeque<Pattern>(patterns);

        List<Callable<Object>> threads = prepareThreads();

        LOGGER.debug("Launching Threads...");
        ThreadUtils.executeThreads(CONFIG.getNumberOfCores(), threads);

        if (NioUtils.isDirEmpty(chunkedDir)) {
            Files.deleteIfExists(chunkedDir);
        }
    }

    private void calculateMemory() {
        LOGGER.debug("Calculating Memory...");
        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMemory =
                (AVAILABLE_MEMORY_PERCENT * totalFreeMemory) / 100;
        readerMemory = availableMemory / CONFIG.getNumberOfCores() / 2;
        writerMemory = availableMemory / CONFIG.getNumberOfCores() / 2;

        LOGGER.debug("totalFreeMemory = %s",
                PrintUtils.humanReadableByteCount(totalFreeMemory, false));
        LOGGER.debug("availableMemory = %s",
                PrintUtils.humanReadableByteCount(availableMemory, false));
        LOGGER.debug("readerMemory    = %s",
                PrintUtils.humanReadableByteCount(readerMemory, false));
        LOGGER.debug("writerMemory    = %s",
                PrintUtils.humanReadableByteCount(writerMemory, false));
    }

    private List<Callable<Object>> prepareThreads() {
        LOGGER.debug("Preparing Threads...");
        List<Callable<Object>> threads = new LinkedList<Callable<Object>>();
        for (int i = 0; i != CONFIG.getNumberOfCores(); ++i) {
            threads.add(new Thread());
        }
        return threads;
    }

}
