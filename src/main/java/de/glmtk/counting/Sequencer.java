package de.glmtk.counting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.pattern.Pattern;

/**
 * Sequencer will spawn {@code 1} {@link SequencerReadTask}, {@code 1}
 * {@link SequencerWriteTask} and {@code max(1, n-2)}
 * {@link SequencerCalculateTask}, where {@code n} is the number of available
 * cores.
 */
public class Sequencer {

    /**
     * How much percent of total free memory to be allocated for sequencer.
     *
     * Careful: Java allocates memory for other tasks, so we can't just set this
     * to 100%. I manually tested estimated this number experimentally.
     */
    private static final int MEMORY_PERCENT = 60;

    /**
     * How much percent of available sequencer memory should be used to buffer
     * reading training data.
     *
     * Note: The sum of READER_MEMORY_PERCENT + WRITER_MEMORY_PERCENT +
     * READ_QUEUE_MEMORY_PERCENT + WRITE_QUEUE_MEMORY_PERCENT has to be 100.
     */
    private static final int READER_MEMORY_PERCENT = 25;

    /**
     * How much percent of available sequencer memory should be used to buffer
     * writing sequence data.
     *
     * Note: The sum of READER_MEMORY_PERCENT + WRITER_MEMORY_PERCENT +
     * READ_QUEUE_MEMORY_PERCENT + WRITE_QUEUE_MEMORY_PERCENT has to be 100.
     */
    private static final int WRITER_MEMORY_PERCENT = 25;

    /**
     * How much percent of available sequencer memory should be used to buffer
     * reading queue items.
     *
     * Note: The sum of READER_MEMORY_PERCENT + WRITER_MEMORY_PERCENT +
     * READ_QUEUE_MEMORY_PERCENT + WRITE_QUEUE_MEMORY_PERCENT has to be 100.
     */
    private static final int READ_QUEUE_MEMORY_PERCENT = 25;

    /**
     * How much percent of available sequencer memory should be used to buffer
     * writing queue items.
     *
     * Note: The sum of READER_MEMORY_PERCENT + WRITER_MEMORY_PERCENT +
     * READ_QUEUE_MEMORY_PERCENT + WRITE_QUEUE_MEMORY_PERCENT has to be 100.
     */
    private static final int WRITE_QUEUE_MEMORY_PERCENT = 25;

    /**
     * Bytes one {@link ReadQueueItem} consumes on average in memory.
     *
     * Estimated this value through averaging memory consumption experimentally.
     */
    private static final long READ_QUEUE_ITEM_MEMORY = 800;

    /**
     * Bytes one (@link {@link WriteQueueItem} consumes on average in memory.
     *
     * Estimated this value through averaging memory consumption experimentally.
     */
    private static final long WRITE_QUEUE_ITEM_MEMORY = 400;

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Sequencer.class);

    /* package */static class ReadQueueItem {

        public Pattern pattern = null;

        public String[] words = null;

        public String[] poses = null;

    }

    /* package */static class WriteQueueItem {

        public Pattern pattern = null;

        public String sequence = null;

    }

    private int numberOfCores;

    private int updateInterval;

    private Map<Integer, Set<Pattern>> patternsByLength;

    private boolean readingDone = false;

    private boolean calculatingDone = false;

    public Sequencer(
            int numberOfCores,
            int updateInterval,
            Set<Pattern> patterns) {
        this.numberOfCores = numberOfCores;
        this.updateInterval = updateInterval;

        patternsByLength = new TreeMap<Integer, Set<Pattern>>();
        for (Pattern pattern : patterns) {
            Set<Pattern> patternsWithLength =
                    patternsByLength.get(pattern.length());
            if (patternsWithLength == null) {
                patternsWithLength = new HashSet<Pattern>();
                patternsByLength.put(pattern.length(), patternsWithLength);
            }
            patternsWithLength.add(pattern);
        }
    }

    public void sequence(Path inputFile, Path outputDir, boolean hasPos)
            throws IOException {
        LOGGER.info("Sequencing '%s' -> '%s'.", inputFile, outputDir);

        Files.createDirectories(outputDir);

        Runtime r = Runtime.getRuntime();
        r.gc();
        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long memory = (MEMORY_PERCENT * totalFreeMemory) / 100;

        long readerMemory = memory * READER_MEMORY_PERCENT / 100;
        long writerMemory = memory * WRITER_MEMORY_PERCENT / 100;
        long readQueueMemory = memory * READ_QUEUE_MEMORY_PERCENT / 100;
        long writeQueueMemory = memory * WRITE_QUEUE_MEMORY_PERCENT / 100;

        BlockingQueue<ReadQueueItem> readQueue =
                new ArrayBlockingQueue<ReadQueueItem>(
                        (int) (readQueueMemory / READ_QUEUE_ITEM_MEMORY));
        BlockingQueue<WriteQueueItem> writeQueue =
                new ArrayBlockingQueue<WriteQueueItem>(
                        (int) (writeQueueMemory / WRITE_QUEUE_ITEM_MEMORY));

        SequencerReadTask readTask =
                new SequencerReadTask(this, readQueue, inputFile,
                        patternsByLength, hasPos, readerMemory, updateInterval);
        List<SequencerCalculateTask> calculateTasks =
                new LinkedList<SequencerCalculateTask>();
        for (int i = 0; i != Math.max(numberOfCores - 2, 1); ++i) {
            calculateTasks.add(new SequencerCalculateTask(this, readQueue,
                    writeQueue));
        }
        SequencerWriteTask writeTask =
                new SequencerWriteTask(this, writeQueue, outputDir,
                        patternsByLength, writerMemory);

        readingDone = false;
        calculatingDone = false;
        try {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            executorService.execute(readTask);
            for (SequencerCalculateTask calculateTask : calculateTasks) {
                executorService.execute(calculateTask);
            }
            executorService.execute(writeTask);

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.info("Sequencer done.");
    }

    /* package */boolean isReadingDone() {
        return readingDone;
    }

    /* package */void readingDone() {
        readingDone = true;
    }

    /* package */boolean isCalculatingDone() {
        return calculatingDone;
    }

    /* package */void calculatingDone() {
        calculatingDone = true;
    }

    /* package */void logPercent(float percent) {
        LOGGER.info("%6.2f%%", percent);
    }

}
