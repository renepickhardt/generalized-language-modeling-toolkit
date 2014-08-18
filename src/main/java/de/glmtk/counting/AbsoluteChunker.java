package de.glmtk.counting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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

/* package */class AbsoluteChunker {

    private static final long B = 1L;

    private static final long KB = 1024 * B;

    private static final long MB = 1024 * KB;

    private static final long CHUNK_MAX_SIZE = 500 * KB;

    private static final long AVERAGE_QUEUE_ITEM_SIZE = 550 * B;

    private static final int AVAILABLE_MEMORY_PERCENT = 50;

    private static final int CHUNK_SIZE_MEMORY_PERCENT = 70;

    private static final int READER_MEMORY_PERCENT = 50;

    private static final int QUEUE_MEMORY_PERCENT = 50;

    private static final Logger LOGGER = LogManager
            .getLogger(AbsoluteChunker.class);

    /* package */static class QueueItem {

        public Pattern pattern = null;

        public String sequence = null;

        public QueueItem(
                Pattern pattern,
                String sequence) {
            this.pattern = pattern;
            this.sequence = sequence;
        }

    }

    private int numberOfCores;

    private int updateInterval;

    private boolean sequencingDone;

    public AbsoluteChunker(
            int numberOfCores,
            int updateInterval) {
        this.numberOfCores = numberOfCores;
        this.updateInterval = updateInterval;
    }

    public void chunk(
            Set<Pattern> patterns,
            Path inputFile,
            Path chunkDir,
            Status status) throws IOException {
        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            return;
        }
        LOGGER.debug("patterns = {}", patterns);
        Files.createDirectories(chunkDir);
        for (Pattern pattern : patterns) {
            Files.createDirectories(chunkDir.resolve(pattern.toString()));
        }

        // Calculate Memory ////////////////////////////////////////////////////
        LOGGER.debug("Calculating Memory...");
        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMemory =
                (AVAILABLE_MEMORY_PERCENT * totalFreeMemory) / 100;

        long chunkMemory =
                Math.min((CHUNK_SIZE_MEMORY_PERCENT * availableMemory) / 100,
                        CHUNK_MAX_SIZE * patterns.size());
        long chunkSize = chunkMemory / patterns.size();

        long remainingMemory = availableMemory - chunkMemory;
        long readerMemory = (READER_MEMORY_PERCENT * remainingMemory) / 100;
        long queueMemory = (QUEUE_MEMORY_PERCENT * remainingMemory) / 100;

        LOGGER.debug("totalFreeMemory = {}MB", totalFreeMemory / MB);
        LOGGER.debug("availableMemory = {}MB", availableMemory / MB);
        LOGGER.debug("readerMemory    = {}MB", readerMemory / MB);
        LOGGER.debug("qeueMemory      = {}MB", queueMemory / MB);
        LOGGER.debug("chunkSize       = {}KB", chunkSize / KB);

        // Prepare Threads /////////////////////////////////////////////////////
        LOGGER.debug("Praparing Threads...");
        Map<Pattern, BlockingQueue<QueueItem>> queues =
                new HashMap<Pattern, BlockingQueue<QueueItem>>();

        AbsoluteChunkerSequencingThread sequencingThread =
                new AbsoluteChunkerSequencingThread(this, queues, patterns,
                        inputFile,
                        status.getTraining() == TrainingStatus.DONE_WITH_POS,
                        readerMemory, updateInterval);

        Queue<Pattern> patternsQueue = new LinkedList<Pattern>(patterns);
        int numQueues = Math.max(1, numberOfCores - 1);
        List<AbsoluteChunkerAggregatingThread> aggregatingThreads =
                new LinkedList<AbsoluteChunkerAggregatingThread>();
        for (int i = 0; i != numQueues; ++i) {
            BlockingQueue<QueueItem> queue =
                    new ArrayBlockingQueue<QueueItem>(
                            (int) (queueMemory / AVERAGE_QUEUE_ITEM_SIZE));
            aggregatingThreads.add(new AbsoluteChunkerAggregatingThread(this,
                    queue, chunkDir, chunkSize, status));

            if (i != numQueues - 1) {
                for (int j = 0; j != patterns.size() / numQueues; ++j) {
                    queues.put(patternsQueue.poll(), queue);
                }
            } else {
                for (Pattern pattern : patternsQueue) {
                    queues.put(pattern, queue);
                }
            }
        }

        // Launch Threads /////////////////////////////////////////////////////
        LOGGER.debug("Launching Threads...");
        sequencingDone = false;
        try {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            executorService.execute(sequencingThread);
            for (AbsoluteChunkerAggregatingThread aggregatingThread : aggregatingThreads) {
                executorService.execute(aggregatingThread);
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isSequencingDone() {
        return sequencingDone;
    }

    public void sequencingIsDone() {
        sequencingDone = true;
    }

}
