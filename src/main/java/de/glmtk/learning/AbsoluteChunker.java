package de.glmtk.learning;

import static de.glmtk.Constants.B;
import static de.glmtk.Constants.KB;
import static de.glmtk.Constants.MB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.Status.TrainingStatus;
import de.glmtk.utils.Pattern;

/* package */class AbsoluteChunker {

    private static final long CHUNK_MAX_SIZE = Constants.CHUNK_MAX_SIZE;

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
            Status status,
            Set<Pattern> patterns,
            Path trainingFile,
            Path absoluteChunkedDir) throws IOException {
        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            return;
        }
        LOGGER.debug("patterns = {}", patterns);
        Files.createDirectories(absoluteChunkedDir);
        for (Pattern pattern : patterns) {
            Files.createDirectories(absoluteChunkedDir.resolve(pattern
                    .toString()));
        }

        int numSequencingThreads = 1;
        int numAggregatingThreads =
                Math.max(1, numberOfCores - numSequencingThreads);
        LOGGER.debug("numSequencingThreads  = {}", numSequencingThreads);
        LOGGER.debug("numAggregatingThreads = {}", numAggregatingThreads);

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
        List<Pattern> reamainingPatterns = new LinkedList<Pattern>(patterns);
        Map<Pattern, BlockingQueue<QueueItem>> patternToAggregatingQueue =
                new HashMap<Pattern, BlockingQueue<QueueItem>>();

        AbsoluteChunkerSequencingThread sequencingThread =
                new AbsoluteChunkerSequencingThread(this,
                        patterns, patternToAggregatingQueue, trainingFile,
                        status.getTraining() == TrainingStatus.DONE_WITH_POS,
                        readerMemory, updateInterval);

        List<AbsoluteChunkerAggregatingThread> aggregatingThreads =
                new LinkedList<AbsoluteChunkerAggregatingThread>();
        for (int i = 0; i != numAggregatingThreads; ++i) {
            BlockingQueue<QueueItem> aggregatingQueue =
                    new ArrayBlockingQueue<QueueItem>(
                            (int) (queueMemory / AVERAGE_QUEUE_ITEM_SIZE));
            if (i != numAggregatingThreads - 1) {
                // If not last thread: have thread receive
                // (patterns.size() / numAggregatings Threads) pattern.
                for (int j = 0; j != patterns.size() / numAggregatingThreads; ++j) {
                    patternToAggregatingQueue.put(reamainingPatterns.remove(0),
                            aggregatingQueue);
                }
            } else {
                // If last thread: have thread receive all remaining patterns.
                for (Pattern pattern : reamainingPatterns) {
                    patternToAggregatingQueue.put(pattern, aggregatingQueue);
                }
            }

            aggregatingThreads.add(new AbsoluteChunkerAggregatingThread(this,
                    status, aggregatingQueue, absoluteChunkedDir, chunkSize));
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
