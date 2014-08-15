package de.glmtk.counting.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.pattern.Pattern;

// TODO: do percentage update
public class AbsoluteCounter {

    /**
     * How much percent of total free memory to be allocated for sequencer.
     *
     * Careful: Java allocates memory for other tasks, so we can't just set this
     * to 100%. I manually tested estimated this number experimentally.
     */
    private static final int MEMORY_PERCENT = 90;

    /**
     * How much percent of available absolute counter memory to be used for
     * reader buffer size.
     */
    private static final int CHUNK_READER_MEMORY_PERCENT = 25;

    /**
     * How much percent of available absolute counter memory to be used for
     * writer buffer size.
     */
    private static final int CHUNK_WRITER_MEMORY_PERCENT = 25;

    /**
     * How much percent of available absolute counter memory to be used for one
     * chunk of counts.
     */
    private static final int CHUNK_MEMORY_SIZE_PERCENT = 50;

    private static final int MERGE_READER_MEMORY_PERCENT = 80;

    private static final int MERGE_WRITER_MEMORY_PERCENT = 20;

    private static final int NUM_PARALLEL_FILES = 10;

    private static final long MB = 1024 * 1024;

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(AbsoluteCounter.class);

    private int numberOfCores;

    private int updateInterval;

    private Set<Pattern> patterns;

    public AbsoluteCounter(
            int numberOfCores,
            int updateInterval,
            Set<Pattern> patterns) {
        this.numberOfCores = numberOfCores;
        this.updateInterval = updateInterval;
        this.patterns = patterns;
    }

    public void count(Path inputDir, Path outputDir) throws IOException {
        LOGGER.info("Absolute counting '%s' -> '%s'.", inputDir, outputDir);

        Files.createDirectories(outputDir);

        Runtime r = Runtime.getRuntime();
        r.gc();
        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long absoluteCounterMemory = (MEMORY_PERCENT * totalFreeMemory) / 100;

        LOGGER.debug("totalFreeMemory       = %sMB", totalFreeMemory / MB);
        LOGGER.debug("absoluteCounterMemory = %sMB", absoluteCounterMemory / MB);

        Map<Pattern, Queue<Path>> chunkedQueues =
                new HashMap<Pattern, Queue<Path>>();
        for (Pattern pattern : patterns) {
            chunkedQueues.put(pattern, new LinkedList<Path>());
        }

        LOGGER.debug("Phase 1/2: Chunking.");
        chunking(inputDir, outputDir, chunkedQueues, absoluteCounterMemory);

        LOGGER.debug("Phase 2/2: Merging.");
        merging(outputDir, chunkedQueues, absoluteCounterMemory);

        LOGGER.info("Absolute counting done.");
    }

    private void chunking(
            Path inputDir,
            Path outputDir,
            Map<Pattern, Queue<Path>> chunkedQueues,
            long memory) {
        long chunkReaderMemory =
                (CHUNK_READER_MEMORY_PERCENT * memory) / 100 / numberOfCores;
        long chunkWriterMemory =
                (CHUNK_WRITER_MEMORY_PERCENT * memory) / 100 / numberOfCores;
        long chunkMemorySize =
                (CHUNK_MEMORY_SIZE_PERCENT * memory) / 100 / numberOfCores;

        LOGGER.debug("chunkReaderMemory = %sMB", chunkReaderMemory / MB);
        LOGGER.debug("chunkWriterMemory = %sMB", chunkWriterMemory / MB);
        LOGGER.debug("chunkMemorySize   = %sMB", chunkMemorySize / MB);

        BlockingQueue<Pattern> patternQueue =
                new LinkedBlockingQueue<Pattern>();
        for (Pattern pattern : patterns) {
            patternQueue.add(pattern);
        }

        List<AbsoluteCounterChunkThread> chunkThreads =
                new LinkedList<AbsoluteCounterChunkThread>();
        for (int i = 0; i != numberOfCores; ++i) {
            chunkThreads.add(new AbsoluteCounterChunkThread(this, patternQueue,
                    chunkedQueues, inputDir, outputDir, chunkReaderMemory,
                    chunkWriterMemory, chunkMemorySize, updateInterval));
        }

        try {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            for (AbsoluteCounterChunkThread chunkThread : chunkThreads) {
                executorService.execute(chunkThread);
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void merging(
            Path outputDir,
            Map<Pattern, Queue<Path>> chunkedQueues,
            long memory) {
        long mergeReaderMemory =
                (MERGE_READER_MEMORY_PERCENT * memory) / 100 / numberOfCores;
        long mergeWriterMemory =
                (MERGE_WRITER_MEMORY_PERCENT * memory) / 100 / numberOfCores;

        LOGGER.debug("mergeReaderMemory = %sMB", mergeReaderMemory / MB);
        LOGGER.debug("mergeWriterMemory = %sMB", mergeWriterMemory / MB);

        BlockingQueue<Pattern> patternQueue =
                new LinkedBlockingQueue<Pattern>();
        for (Pattern pattern : patterns) {
            patternQueue.add(pattern);
        }

        List<AbsoluteCounterMergeThread> mergeThreads =
                new LinkedList<AbsoluteCounterMergeThread>();
        for (int i = 0; i != numberOfCores; ++i) {
            mergeThreads.add(new AbsoluteCounterMergeThread(this, patternQueue,
                    chunkedQueues, outputDir, mergeReaderMemory,
                    mergeWriterMemory, updateInterval, NUM_PARALLEL_FILES));
        }

        try {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            for (AbsoluteCounterMergeThread mergeThread : mergeThreads) {
                executorService.execute(mergeThread);
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
