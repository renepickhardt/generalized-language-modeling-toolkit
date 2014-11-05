package de.glmtk.counting;

import static de.glmtk.Constants.B;
import static de.glmtk.Constants.KB;
import static de.glmtk.Constants.MB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;

/* package */class ContinuationChunker {

    private static final long CHUNK_MAX_SIZE = Constants.CHUNK_MAX_SIZE;

    private static final long AVERAGE_QUEUE_ITEM_SIZE = 580 * B;

    private static final int AVAILABLE_MEMORY_PERCENT = 80;

    private static final int CHUNK_SIZE_MEMORY_PERCENT = 70;

    private static final Logger LOGGER = LogManager
            .getLogger(ContinuationChunker.class);

    private static final Comparator<Pattern> SOURCE_PATTERN_COMPARATOR =
            new Comparator<Pattern>() {

        @Override
        public int compare(Pattern a, Pattern b) {
            return ((Integer) a.numElems(PatternElem.CSKIP_ELEMS))
                            .compareTo(b.numElems(PatternElem.CSKIP_ELEMS));
        }

    };

    /* package */static class QueueItem {

        public Pattern pattern;

        public String sequence;

        public long count;

        public QueueItem(
                Pattern pattern,
                String sequence,
                long count) {
            this.pattern = pattern;
            this.sequence = sequence;
            this.count = count;
        }

    }

    private int numberOfCores;

    private int updateInterval;

    private int numActiveSequencingThreads;

    public ContinuationChunker(
            int numberOfCores,
            int updateInterval) {
        this.numberOfCores = numberOfCores;
        this.updateInterval = updateInterval;
    }

    public void chunk(
            Status status,
            Set<Pattern> patterns,
            Path absoluteCountedDir,
            Path absoluteChunkedDir,
            Path continuationCountedDir,
            Path continuationChunkedDir) throws IOException {
        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            return;
        }
        LOGGER.debug("patterns = {}", patterns);
        Files.createDirectories(continuationChunkedDir);
        for (Pattern pattern : patterns) {
            Files.createDirectories(continuationChunkedDir.resolve(pattern
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

        long readerMemory = chunkSize;
        long queueMemory =
                (availableMemory - chunkMemory - readerMemory
                        * numSequencingThreads)
                        / numAggregatingThreads;

        LOGGER.debug("totalFreeMemory = {}MB", totalFreeMemory / MB);
        LOGGER.debug("availableMemory = {}MB", availableMemory / MB);
        LOGGER.debug("readerMemory    = {}KB", readerMemory / KB);
        LOGGER.debug("queueMemory     = {}MB", queueMemory / MB);
        LOGGER.debug("chunkSize       = {}KB", chunkSize / KB);

        // Prepare Threads /////////////////////////////////////////////////////
        LOGGER.debug("Preparing Threads...");

        Map<Pattern, Pattern> patternToSource = new HashMap<Pattern, Pattern>();
        for (Pattern pattern : patterns) {
            patternToSource.put(pattern, pattern.getContinuationSource());
        }
        Map<Pattern, List<Pattern>> sourceToPattern =
                computeSourceToPattern(patternToSource, status);

        List<BlockingQueue<QueueItem>> aggregatingQueues =
                new ArrayList<BlockingQueue<QueueItem>>(numAggregatingThreads);
        List<Map<Pattern, List<Pattern>>> aggregatingSourceToPattern =
                new ArrayList<Map<Pattern, List<Pattern>>>(
                        numAggregatingThreads);
        Map<Pattern, BlockingQueue<QueueItem>> sourceToAggregatingQueue =
                new HashMap<Pattern, BlockingQueue<QueueItem>>();
        setupThreadParameters(numAggregatingThreads, queueMemory,
                sourceToPattern, aggregatingQueues, aggregatingSourceToPattern,
                sourceToAggregatingQueue);

        PriorityBlockingQueue<Pattern> sourcePatternsQueue =
                new PriorityBlockingQueue<Pattern>(sourceToPattern.size(),
                        SOURCE_PATTERN_COMPARATOR);
        sourcePatternsQueue.addAll(sourceToPattern.keySet());

        List<ContinuationChunkerSequencingThread> sequencingThreads =
                new LinkedList<ContinuationChunkerSequencingThread>();
        for (int i = 0; i != numSequencingThreads; ++i) {
            sequencingThreads.add(new ContinuationChunkerSequencingThread(this,
                    status, sourcePatternsQueue, sourceToAggregatingQueue,
                    absoluteCountedDir, absoluteChunkedDir,
                    continuationCountedDir, continuationChunkedDir,
                    readerMemory, updateInterval));
        }

        List<ContinuationChunkerAggregatingThread> aggregatingThreads =
                new LinkedList<ContinuationChunkerAggregatingThread>();
        for (int i = 0; i != numAggregatingThreads; ++i) {
            aggregatingThreads.add(new ContinuationChunkerAggregatingThread(
                    this, status, aggregatingQueues.get(i),
                    aggregatingSourceToPattern.get(i), continuationChunkedDir,
                    chunkSize));
        }

        // Launch Threads //////////////////////////////////////////////////////
        LOGGER.debug("Launching Threads...");
        numActiveSequencingThreads = numSequencingThreads;
        try {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            for (ContinuationChunkerSequencingThread sequencingThread : sequencingThreads) {
                executorService.execute(sequencingThread);
            }
            for (ContinuationChunkerAggregatingThread aggregatingThread : aggregatingThreads) {
                executorService.execute(aggregatingThread);
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<Pattern, List<Pattern>> computeSourceToPattern(
            Map<Pattern, Pattern> patternToSource,
            Status status) {
        Set<Pattern> possiblePatterns = new HashSet<Pattern>();
        Set<Pattern> availablePatterns = new HashSet<Pattern>();
        availablePatterns.addAll(status.getChunkedPatterns(false));
        availablePatterns.addAll(status.getCounted(false));
        availablePatterns.addAll(status.getChunkedPatterns(true));
        availablePatterns.addAll(status.getCounted(true));

        Map<Pattern, List<Pattern>> result =
                new HashMap<Pattern, List<Pattern>>();
        while (!patternToSource.isEmpty()) {
            boolean changed = false;

            Iterator<Map.Entry<Pattern, Pattern>> i =
                    patternToSource.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Pattern, Pattern> entry = i.next();
                Pattern pattern = entry.getKey();
                Pattern source = entry.getValue();

                if (availablePatterns.contains(source)
                        || possiblePatterns.contains(source)) {
                    List<Pattern> patterns = result.get(source);
                    if (patterns == null) {
                        patterns = new LinkedList<Pattern>();
                        result.put(source, patterns);
                    }
                    patterns.add(pattern);
                    possiblePatterns.add(pattern);
                    i.remove();
                    changed = true;
                }
            }

            if (!changed) {
                LOGGER.error(
                        "Cannot not calculate continuation counts for these patterns, since source patterns are not present: {}.",
                        patternToSource.keySet());
                break;
            }
        }
        return result;
    }

    private void setupThreadParameters(
            int numAggregatingThreads,
            long queueMemory,
            Map<Pattern, List<Pattern>> sourceToPattern,
            List<BlockingQueue<QueueItem>> aggregatingQueues,
            List<Map<Pattern, List<Pattern>>> aggregatingSourceToPattern,
            Map<Pattern, BlockingQueue<QueueItem>> sourceToAggregatingQueues) {
        for (int i = 0; i != numAggregatingThreads; ++i) {
            aggregatingQueues.add(new ArrayBlockingQueue<QueueItem>(
                    (int) (queueMemory / AVERAGE_QUEUE_ITEM_SIZE)));
            aggregatingSourceToPattern
            .add(new HashMap<Pattern, List<Pattern>>());
        }

        int threadIter = 0;
        for (Map.Entry<Pattern, List<Pattern>> entry : sourceToPattern
                .entrySet()) {
            Pattern source = entry.getKey();
            aggregatingSourceToPattern.get(threadIter).put(source,
                    entry.getValue());
            sourceToAggregatingQueues.put(source,
                    aggregatingQueues.get(threadIter));
            threadIter = (threadIter + 1) % numAggregatingThreads;
        }
    }

    public boolean isSequencingDone() {
        return numActiveSequencingThreads == 0;
    }

    public void sequencingThreadDone() {
        --numActiveSequencingThreads;
    }

}
