package de.glmtk.counting.absolute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.Status.TrainingStatus;
import de.glmtk.pattern.Pattern;

public class Chunker {

    private static final long KB = 1024;

    private static final long MB = 1024 * KB;

    private static final long CHUNK_MAX_SIZE = 500 * KB;

    private static final long AVERAGE_READ_QUEUE_ITEM_SIZE = 1000;

    private static final long AVERAGE_WRITE_QUEUE_ITEM_SIZE = 550;

    private static final int AVAILABLE_MEMORY_PERCENT = 50;

    private static final int CHUNK_SIZE_MEMORY_PERCENT = 70;

    private static final int READER_MEMORY_PERCENT = 33;

    private static final int READ_QUEUE_MEMORY_PERCENT = 33;

    private static final int WRITE_QUEUE_MEMORY_PERCENT = 33;

    private static final Logger LOGGER = LogManager.getLogger(Chunker.class);

    /* package */static class ReadQueueItem {

        public Pattern pattern;

        public String[] words;

        public String[] poses;

        public ReadQueueItem(
                Pattern pattern,
                String[] words,
                String[] poses) {
            this.pattern = pattern;
            this.words = words;
            this.poses = poses;
        }

    }

    /* package */static class WriteQueueItem {

        public Pattern pattern = null;

        public String sequence = null;

        public WriteQueueItem(
                Pattern pattern,
                String sequence) {
            this.pattern = pattern;
            this.sequence = sequence;
        }

    }

    private int numberOfCores;

    private int updateInterval;

    private boolean readingDone;

    private boolean processingDone;

    /* package */Chunker(
            int numberOfCores,
            int updateInterval) {
        this.numberOfCores = numberOfCores;
        this.updateInterval = updateInterval;
    }

    public void chunk(
            Set<Pattern> patterns,
            Path inputFile,
            Path outputDir,
            Status status) throws IOException {
        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            return;
        }
        LOGGER.debug("patterns = " + patterns);
        for (Pattern pattern : patterns) {
            Files.createDirectories(outputDir.resolve(pattern.toString()));
        }

        // Calculate Memory ////////////////////////////////////////////////////
        Runtime r = Runtime.getRuntime();
        r.gc();
        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMemory =
                (AVAILABLE_MEMORY_PERCENT * totalFreeMemory) / 100;
        long chunkMemory =
                Math.min((CHUNK_SIZE_MEMORY_PERCENT * availableMemory) / 100,
                        CHUNK_MAX_SIZE * patterns.size());
        long chunkSize = chunkMemory / patterns.size();
        long writerMemory = chunkSize;
        long remainingMemory = availableMemory - chunkMemory;
        long readerMemory = (READER_MEMORY_PERCENT * remainingMemory) / 100;
        long readQueueMemory =
                (READ_QUEUE_MEMORY_PERCENT * remainingMemory) / 100;
        long writeQueueMemory =
                (WRITE_QUEUE_MEMORY_PERCENT * remainingMemory) / 100;

        LOGGER.debug("totalFreeMemory  = {}MB", totalFreeMemory / MB);
        LOGGER.debug("availableMemory  = {}MB", availableMemory / MB);
        LOGGER.debug("readerMemory     = {}MB", readerMemory / MB);
        LOGGER.debug("writerMemory     = {}MB", writerMemory / MB);
        LOGGER.debug("readQueueMemory  = {}MB", readQueueMemory / MB);
        LOGGER.debug("writeQueueMemory = {}MB", writeQueueMemory / MB);
        LOGGER.debug("chunkSize        = {}KB", chunkSize / KB);

        // Prepare Threads /////////////////////////////////////////////////////
        BlockingQueue<ReadQueueItem> readQueue =
                new ArrayBlockingQueue<ReadQueueItem>(
                        (int) (readQueueMemory / AVERAGE_READ_QUEUE_ITEM_SIZE));
        BlockingQueue<WriteQueueItem> writeQueue =
                new ArrayBlockingQueue<WriteQueueItem>(
                        (int) (writeQueueMemory / AVERAGE_WRITE_QUEUE_ITEM_SIZE));

        ChunkerReadingThread readingThread =
                new ChunkerReadingThread(this, readQueue, patterns, inputFile,
                        status.getTraining() == TrainingStatus.DONE_WITH_POS,
                        readerMemory, updateInterval);
        List<ChunkerProcessingThread> processingThreads =
                new LinkedList<ChunkerProcessingThread>();
        for (int i = 0; i != Math.max(1, numberOfCores - 2); ++i) {
            processingThreads.add(new ChunkerProcessingThread(this, readQueue,
                    writeQueue));
        }
        ChunkerAggregatingThread aggregatingThread =
                new ChunkerAggregatingThread(this, writeQueue, outputDir,
                        chunkSize, status);

        // Launch Threads //////////////////////////////////////////////////////
        readingDone = false;
        processingDone = false;
        try {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            executorService.execute(readingThread);
            for (ChunkerProcessingThread processingThread : processingThreads) {
                executorService.execute(processingThread);
            }
            executorService.execute(aggregatingThread);

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isReadingDone() {
        return readingDone;
    }

    public void readingIsDone() {
        readingDone = true;
    }

    public boolean isProcessingDone() {
        return processingDone;
    }

    public void processingIsDone() {
        processingDone = true;
    }

}
