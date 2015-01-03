package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

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
import de.glmtk.util.ExceptionUtils;
import de.glmtk.util.NioUtils;
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

    private class Thread implements Callable<Object> {

        @Override
        public Object call() throws InterruptedException, IOException {
            while (!patternQueue.isEmpty()) {
                Pattern pattern =
                        patternQueue.poll(Constants.QUEUE_TIMEOUT,
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
            while ((chunksForPattern =
                    status.getChunksForPattern(continuation, pattern)).size() != 1) {
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
                    status.performMergeForChunks(continuation, pattern,
                            chunksToMerge, mergeFile.getFileName());
                } catch (IOException e) {
                    // Updating status did not work, we continue in the hope
                    // it works next time.
                    LOGGER.warn("Unable to write status, continuing: "
                            + ExceptionUtils.getStackTrace(e));
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
            status.finishMerge(continuation, pattern);

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

    private int numParallelReaders = 10;

    private Progress progress;

    private boolean continuation;

    private Status status;

    private Path chunkedDir;

    private Path countedDir;

    private BlockingQueue<Pattern> patternQueue;

    private long readerMemory;

    private long writerMemory;

    public void mergeAbsolute(
            Status status,
            Set<Pattern> patterns,
            Path chunkedDir,
            Path countedDir) throws Exception {
        OUTPUT.setPhase(Phase.ABSOLUTE_MERGING, true);
        merge(false, status, patterns, chunkedDir, countedDir);
    }

    public void mergeContinuation(
            Status status,
            Set<Pattern> patterns,
            Path chunkedDir,
            Path countedDir) throws Exception {
        OUTPUT.setPhase(Phase.CONTINUATION_MERGING, true);
        merge(true, status, patterns, chunkedDir, countedDir);
    }

    private void merge(
            boolean continuation,
            Status status,
            Set<Pattern> patterns,
            Path chunkedDir,
            Path countedDir) throws Exception {
        LOGGER.debug("patterns = %s", patterns);
        progress = new Progress(patterns.size());
        if (patterns.isEmpty()) {
            LOGGER.debug("No chunks to merge, returning.");
            progress.set(1.0);
            return;
        }

        Files.createDirectories(countedDir);

        this.continuation = continuation;
        this.status = status;
        this.chunkedDir = chunkedDir;
        this.countedDir = countedDir;
        patternQueue = new LinkedBlockingDeque<Pattern>(patterns);
        calculateMemory();

        List<Callable<Object>> threads = new LinkedList<Callable<Object>>();
        for (int i = 0; i != CONFIG.getNumberOfCores(); ++i) {
            threads.add(new Thread());
        }
        ThreadUtils.executeThreads(CONFIG.getNumberOfCores(), threads);

        if (NioUtils.isDirEmpty(chunkedDir)) {
            Files.deleteIfExists(chunkedDir);
        }
    }

    private void calculateMemory() {
        double AVAILABLE_MEM_RATIO = 0.35;

        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMem = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMem = (long) (AVAILABLE_MEM_RATIO * totalFreeMem);
        long memPerThread = availableMem / CONFIG.getNumberOfCores();

        readerMemory = memPerThread / 2;
        writerMemory = memPerThread - readerMemory;

        LOGGER.debug("totalFreeMem = %s", humanReadableByteCount(totalFreeMem));
        LOGGER.debug("availableMem = %s", humanReadableByteCount(availableMem));
        LOGGER.debug("memPerThread = %s", humanReadableByteCount(memPerThread));
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }

}
