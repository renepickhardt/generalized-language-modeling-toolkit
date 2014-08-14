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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Logging;
import de.glmtk.counting.Sequencer.ReadQueueItem;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

public class SequencerReadTask implements Runnable {

    private static final Logger LOGGER = LogManager
            .getLogger(SequencerReadTask.class);

    private static final long IDLE_TIME = 10;

    private Sequencer sequencer;

    private BlockingQueue<ReadQueueItem> readQueue;

    private Path inputFile;

    private Map<Integer, Set<Pattern>> patternsByLength;

    private boolean hasPos;

    private long readerMemory;

    private int updateInterval;

    public SequencerReadTask(
            Sequencer sequencer,
            BlockingQueue<ReadQueueItem> readQueue,
            Path inputFile,
            Map<Integer, Set<Pattern>> patternsByLength,
            boolean hasPos,
            long readerMemory,
            int updateInterval) {
        this.sequencer = sequencer;
        this.readQueue = readQueue;
        this.inputFile = inputFile;
        this.patternsByLength = patternsByLength;
        this.hasPos = hasPos;
        this.readerMemory = readerMemory;
        this.updateInterval = updateInterval;
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

                String[] split =
                        StringUtils.splitAtChar(line, ' ').toArray(
                                new String[0]);
                String[] words = new String[split.length];
                String[] poses = new String[split.length];
                extractWordAndPosesFromSplit(split, words, poses);
                generateReadStatusItems(words, poses);

                if (updateInterval != 0) {
                    long t = System.currentTimeMillis();
                    if (t - time >= updateInterval) {
                        time = t;
                        sequencer.logPercent(100.0f * readSize / totalSize);
                    }
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        sequencer.readingDone();
        LOGGER.debug("Sequencer reading done.");
    }

    private void extractWordAndPosesFromSplit(
            String[] split,
            String[] words,
            String[] poses) {
        for (int i = 0; i != split.length; ++i) {
            String word = split[i];
            if (hasPos) {
                int lastSlash = word.lastIndexOf('/');
                if (lastSlash == -1) {
                    words[i] = word;
                    poses[i] = "UNKP"; // Unkown POS, not part of any POS-tagset.
                } else {
                    words[i] = word.substring(0, lastSlash);
                    poses[i] = word.substring(lastSlash + 1);
                }
            } else {
                words[i] = word;
            }
        }
    }

    private void generateReadStatusItems(String[] words, String[] poses)
            throws InterruptedException {
        for (Map.Entry<Integer, Set<Pattern>> entry : patternsByLength
                .entrySet()) {
            int patternLength = entry.getKey();
            Set<Pattern> patterns = entry.getValue();
            for (int i = 0; i <= words.length - patternLength; ++i) {
                String[] w = new String[patternLength];
                String[] p = new String[patternLength];
                System.arraycopy(words, i, w, 0, patternLength);
                System.arraycopy(poses, i, p, 0, patternLength);
                for (Pattern pattern : patterns) {
                    ReadQueueItem ritem = new ReadQueueItem();
                    ritem.pattern = pattern;
                    ritem.words = w;
                    ritem.poses = p;
                    while (!readQueue.offer(ritem, IDLE_TIME,
                            TimeUnit.MILLISECONDS)) {
                        LOGGER.trace("SequencerReadTask idle.");
                        if (Logging.getLogLevel() == Level.ALL
                                || Logging.getLogLevel() == Level.TRACE) {
                            StatisticalNumberHelper
                                    .count("IdleSequencerReadTask");
                        }
                    }

                    // To get memory average of ReadQueueItem. Don't forget to:
                    // - add import
                    // - uncomment classmexer.jar in pom
                    // - add javaagent in run.sh to MAVEN_OPTS.
                    //StatisticalNumberHelper.add("ReadQueueItem", MemoryUtil
                    //        .deepMemoryUsageOf(ritem, VisibilityFilter.ALL));
                }
            }
        }
    }

}
