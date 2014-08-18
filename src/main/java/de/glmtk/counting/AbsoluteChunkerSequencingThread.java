package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.counting.AbsoluteChunker.QueueItem;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

/* package */class AbsoluteChunkerSequencingThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 10;

    private static final String UNKOWN_POS = "UNKP";

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(AbsoluteChunkerSequencingThread.class);

    private AbsoluteChunker absoluteChunker;

    private Map<Pattern, BlockingQueue<QueueItem>> queues;

    private Path inputFile;

    private boolean inputFileHasPos;

    private long readerMemory;

    private int updateInterval;

    private Map<Integer, Set<Pattern>> patternsByLength;

    public AbsoluteChunkerSequencingThread(
            AbsoluteChunker absoluteChunker,
            Map<Pattern, BlockingQueue<QueueItem>> queues,
            Set<Pattern> patterns,
            Path inputFile,
            boolean inputFileHasPos,
            long readerMemory,
            int updateInterval) {
        this.absoluteChunker = absoluteChunker;
        this.queues = queues;
        this.inputFile = inputFile;
        this.inputFileHasPos = inputFileHasPos;
        this.readerMemory = readerMemory;
        this.updateInterval = updateInterval;

        patternsByLength = new HashMap<Integer, Set<Pattern>>();
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

    @Override
    public void run() {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Files.newInputStream(inputFile)), (int) readerMemory)) {
            long readSize = 0;
            long totalSize = Files.size(inputFile);
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

                String[] split =
                        StringUtils.splitAtChar(line, ' ').toArray(
                                new String[0]);
                String[] words = new String[split.length];
                String[] poses = new String[split.length];
                extractWordsAndPoses(split, words, poses);
                generateAndQueueSequences(words, poses);
            }

            absoluteChunker.sequencingIsDone();
            LOGGER.debug("{} finished.",
                    AbsoluteChunkerSequencingThread.class.getSimpleName());
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void extractWordsAndPoses(
            String[] split,
            String[] words,
            String[] poses) {
        for (int i = 0; i != split.length; ++i) {
            String word = split[i];
            if (inputFileHasPos) {
                int lastSlash = word.lastIndexOf('/');
                if (lastSlash == -1) {
                    words[i] = word;
                    poses[i] = UNKOWN_POS;
                } else {
                    words[i] = word.substring(0, lastSlash);
                    poses[i] = word.substring(lastSlash + 1);
                }
            } else {
                words[i] = word;
                poses[i] = UNKOWN_POS;
            }
        }
    }

    private void generateAndQueueSequences(String[] words, String[] poses)
            throws InterruptedException {
        for (Map.Entry<Integer, Set<Pattern>> entry : patternsByLength
                .entrySet()) {
            int patternLength = entry.getKey();
            Set<Pattern> patterns = entry.getValue();
            for (int p = 0; p <= words.length - patternLength; ++p) {
                for (Pattern pattern : patterns) {
                    String sequence = pattern.apply(words, poses, p);
                    QueueItem item = new QueueItem(pattern, sequence);
                    while (!queues.get(pattern).offer(item, QUEUE_WAIT_TIME,
                            TimeUnit.MILLISECONDS)) {
                        LOGGER.trace("ChunkerReadingThread idle, because queue full.");
                        StatisticalNumberHelper
                                .count("Idle ChunkerReadingThread because queue full");
                    }

                    // To get memory average of ReadQueueItem. Don't forget to:
                    // - add import
                    // - uncomment classmexer.jar in pom
                    // - add javaagent in run.sh to MAVEN_OPTS.
                    //StatisticalNumberHelper.average("ReadQueueItem", MemoryUtil
                    //        .deepMemoryUsageOf(witem, VisibilityFilter.ALL));
                }
            }
        }
    }
}
