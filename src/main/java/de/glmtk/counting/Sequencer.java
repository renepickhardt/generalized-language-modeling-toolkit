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

public class Sequencer {

    /* package */static class ReadQueueItem {

        public Pattern pattern = null;

        public String[] words = null;

        public String[] poses = null;

    }

    /* package */static class WriteQueueItem {

        public Pattern pattern = null;

        public String sequence = null;

    }

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Sequencer.class);

    private static final int MEMORY_PERCENT = 60;

    /**
     * Estimated this value through averaging memory consumption experimentally.
     */
    private static final long READ_QUEUE_ITEM_MEMORY = 800;

    /**
     * Estimated this value through averaging memory consumption experimentally.
     */
    private static final long WRITE_QUEUE_ITEM_MEMORY = 400;

    private int numberOfCores;

    private Map<Integer, Set<Pattern>> patternsByLength;

    private boolean readingDone = false;

    private boolean calculatingDone = false;

    // TODO: Add percent display.
    public Sequencer(
            int numberOfCores,
            Set<Pattern> patterns) {
        this.numberOfCores = numberOfCores;

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
        LOGGER.info("Sequencing: %s -> %s", inputFile, outputDir);

        Files.createDirectories(outputDir);

        Runtime r = Runtime.getRuntime();
        r.gc();
        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long memory = (MEMORY_PERCENT * totalFreeMemory) / 100;

        long readerMemory = memory * 25 / 100;
        long writerMemory = memory * 25 / 100;
        long readQueueMemory = memory * 25 / 100;
        long writeQueueMemory = memory * 25 / 100;

        BlockingQueue<ReadQueueItem> readQueue =
                new ArrayBlockingQueue<ReadQueueItem>(
                        (int) (readQueueMemory / READ_QUEUE_ITEM_MEMORY));
        BlockingQueue<WriteQueueItem> writeQueue =
                new ArrayBlockingQueue<WriteQueueItem>(
                        (int) (writeQueueMemory / WRITE_QUEUE_ITEM_MEMORY));

        SequencerReadTask readTask =
                new SequencerReadTask(this, readQueue, inputFile,
                        patternsByLength, hasPos, readerMemory);
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

        LOGGER.info("Sequencer: done.");
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

}
