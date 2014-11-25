package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

import de.glmtk.Constants;
import de.glmtk.counting.AbsoluteChunker.QueueItem;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.Patterns;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

/* package */class AbsoluteChunkerSequencingThread implements Runnable {

    private static final long QUEUE_IDLE_TIME = Constants.QUEUE_IDLE_TIME;

    private static final String CLASS_NAME =
            AbsoluteChunkerSequencingThread.class.getSimpleName();

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(AbsoluteChunkerSequencingThread.class);

    private AbsoluteChunker absoluteChunker;

    private Map<Integer, Set<Pattern>> patternsBySize;

    private Map<Pattern, BlockingQueue<QueueItem>> aggregatingQueues;

    private Path trainingFile;

    private boolean trainingFileHasPos;

    private long readerMemory;

    private int updateInterval;

    public AbsoluteChunkerSequencingThread(
            AbsoluteChunker absoluteChunker,
            Set<Pattern> patterns,
            Map<Pattern, BlockingQueue<QueueItem>> aggregatingQueues,
            Path trainingFile,
            boolean trainingFileHasPos,
            long readerMemory,
            int updateInterval) {
        this.absoluteChunker = absoluteChunker;
        this.aggregatingQueues = aggregatingQueues;
        this.trainingFile = trainingFile;
        this.trainingFileHasPos = trainingFileHasPos;
        this.readerMemory = readerMemory;
        this.updateInterval = updateInterval;
        patternsBySize = Patterns.groupPatternsBySize(patterns);
    }

    @Override
    public void run() {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Files.newInputStream(trainingFile)), (int) readerMemory)) {
            long readSize = 0;
            long totalSize = Files.size(trainingFile);
            long time = System.currentTimeMillis();

            String line;
            while ((line = reader.readLine()) != null) {
                readSize += line.getBytes().length;
                if (updateInterval != 0) {
                    long curTime = System.currentTimeMillis();
                    if (curTime - time >= updateInterval) {
                        time = curTime;
                        LOGGER.info("%6.2f%%", 100.0f * readSize / totalSize);
                    }
                }

                generateAndQueueSequences(line);
            }

            absoluteChunker.sequencingIsDone();
            LOGGER.debug("Done.");
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void generateAndQueueSequences(String line)
            throws InterruptedException {
        String[] split =
                StringUtils.splitAtChar(line, ' ').toArray(new String[0]);
        String[] words = new String[split.length];
        String[] poses = new String[split.length];
        StringUtils.extractWordsAndPoses(split, trainingFileHasPos, words,
                poses);

        for (Map.Entry<Integer, Set<Pattern>> entry : patternsBySize.entrySet()) {
            int patternSize = entry.getKey();
            Set<Pattern> patterns = entry.getValue();
            for (int p = 0; p <= split.length - patternSize; ++p) {
                for (Pattern pattern : patterns) {
                    String sequence = pattern.apply(words, poses, p);

                    QueueItem item = new QueueItem(pattern, sequence);
                    while (!aggregatingQueues.get(pattern).offer(item,
                            QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS)) {
                        LOGGER.trace("Idle, because queue full.");
                        StatisticalNumberHelper.count(CLASS_NAME
                                + " idle (queue full)");
                    }

                    if (Constants.DEBUG_AVERAGE_MEMORY) {
                        StatisticalNumberHelper.average(
                                "AbsoluteChunker.QueueItem Memory", MemoryUtil
                                .deepMemoryUsageOf(item,
                                        VisibilityFilter.ALL));
                    }
                }
            }
        }
    }
}
