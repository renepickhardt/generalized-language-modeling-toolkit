package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StringUtils;

/**
 * Implementation inspired from <a href=
 * "https://code.google.com/p/externalsortinginjava/source/browse/trunk/src/main/java/com/google/code/externalsorting/ExternalSort.java"
 * >ExternalSort</a>
 */
public class AbsoluteCounterMergeThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 10;

    private static final Logger LOGGER = LogManager
            .getLogger(AbsoluteCounterMergeThread.class);

    @SuppressWarnings("unused")
    private AbsoluteCounter absoluteCounter;

    private BlockingQueue<Pattern> patternQueue;

    private Map<Pattern, Queue<Path>> chunkedQueues;

    private Path outputDir;

    private long mergeReaderMemory;

    private long mergeWriterMemory;

    @SuppressWarnings("unused")
    private int updateInterval;

    private int numParallelFiles;

    public AbsoluteCounterMergeThread(
            AbsoluteCounter absoluteCounter,
            BlockingQueue<Pattern> patternQueue,
            Map<Pattern, Queue<Path>> chunkedQueues,
            Path outputDir,
            long mergeReaderMemory,
            long mergeWriterMemory,
            int updateInterval,
            int numParallelFiles) {
        this.absoluteCounter = absoluteCounter;
        this.patternQueue = patternQueue;
        this.chunkedQueues = chunkedQueues;
        this.outputDir = outputDir;
        this.mergeReaderMemory = mergeReaderMemory;
        this.mergeWriterMemory = mergeWriterMemory;
        this.updateInterval = updateInterval;
        this.numParallelFiles = numParallelFiles;
    }

    @Override
    public void run() {
        try {
            while (!patternQueue.isEmpty()) {
                Pattern pattern =
                        patternQueue.poll(QUEUE_WAIT_TIME,
                                TimeUnit.MILLISECONDS);
                if (pattern != null) {
                    LOGGER.debug("Merging pattern: " + pattern);
                    merge(pattern);
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("AbsoluteCounterMergeThread finished.");
    }

    private void merge(Pattern pattern) throws IOException {
        Path tmpDir = outputDir.resolve(pattern + ".tmp");
        Files.createDirectories(tmpDir);

        int mergeCounter = 0;
        Queue<Path> chunks = chunkedQueues.get(pattern);

        while (true) {
            Queue<Path> curChunks = new LinkedList<Path>();
            for (int i = 0; i != numParallelFiles && !chunks.isEmpty(); ++i) {
                curChunks.add(chunks.poll());
            }

            if (curChunks.size() == 1) {
                Path chunk = curChunks.poll();
                Path dest = outputDir.resolve(pattern.toString());
                LOGGER.trace("Finishing pattern {}: {} -> {}.", pattern, chunk,
                        dest);
                Files.deleteIfExists(dest);
                Files.move(chunk, dest);
                break;
            }

            Path mergeFile = tmpDir.resolve("merge" + mergeCounter);

            LOGGER.trace("Merging pattern {}: {} -> {}.", pattern, curChunks,
                    mergeFile);

            mergeChunksToFile(curChunks, mergeFile);

            chunks.add(mergeFile);
            ++mergeCounter;
        }
    }

    private void mergeChunksToFile(Queue<Path> curChunks, Path mergeFile)
            throws IOException {
        Files.deleteIfExists(mergeFile);
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(
                        Files.newOutputStream(mergeFile)),
                        (int) mergeWriterMemory)) {
            PriorityQueue<SequenceCountReader> readerQueue =
                    new PriorityQueue<SequenceCountReader>(numParallelFiles,
                            new SequenceCountReaderComparator());
            for (Path chunk : curChunks) {
                readerQueue.add(new SequenceCountReader(new BufferedReader(
                        new InputStreamReader(Files.newInputStream(chunk)),
                        (int) mergeReaderMemory / numParallelFiles)));
            }

            String sequence = null;
            Long count = null;
            while (!readerQueue.isEmpty()) {
                SequenceCountReader reader = readerQueue.poll();
                String s = reader.getSequence();
                Long c = reader.getCount();

                if (s.equals(sequence)) {
                    count += c;
                } else {
                    if (sequence != null) {
                        writer.write(sequence);
                        writer.write('\t');
                        writer.write(count.toString());
                        writer.write('\n');
                    }
                    sequence = s;
                    count = c;
                }
                reader.pop();

                if (reader.isEmpty()) {
                    reader.close();
                } else {
                    readerQueue.add(reader);
                }
            }
        }
    }

    private static class SequenceCountReader implements AutoCloseable,
    Comparable<SequenceCountReader> {

        public BufferedReader reader;

        public String sequence;

        public Long count;

        public SequenceCountReader(
                BufferedReader reader) throws IOException {
            this.reader = reader;
            pop();
        }

        private void pop() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                sequence = null;
                count = null;
            } else {
                List<String> split = StringUtils.splitAtChar(line, '\t');
                sequence = split.get(0);
                count = Long.valueOf(split.get(1));
            }
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        public boolean isEmpty() {
            return getSequence() == null;
        }

        public String getSequence() {
            return sequence;
        }

        public Long getCount() {
            return count;
        }

        @Override
        public int compareTo(SequenceCountReader other) {
            return getSequence().compareTo(other.getSequence());
        }

    }

    private static class SequenceCountReaderComparator implements
            Comparator<SequenceCountReader> {

        @Override
        public int compare(SequenceCountReader a, SequenceCountReader b) {
            return a.compareTo(b);
        }

    }

}
