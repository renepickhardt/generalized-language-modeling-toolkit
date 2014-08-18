package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

import de.glmtk.Status;
import de.glmtk.counting.ContinuationChunker.QueueItem;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

/* package */class ContinuationChunkerSequencingThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 10;

    private static final int SLEEP_TIME = 10;

    private static final Logger LOGGER = LogManager
            .getLogger(ContinuationChunkerSequencingThread.class);

    private ContinuationChunker continuationChunker;

    private BlockingQueue<Pattern> patternsQueue;

    private Map<Pattern, BlockingQueue<QueueItem>> aggregatingQueues;

    private Path absoluteCountedDir;

    private Path absoluteChunkedDir;

    private Path continuationCountedDir;

    private Path continuationChunkedDir;

    private long readerMemory;

    @SuppressWarnings("unused")
    private int updateInterval;

    private Status status;

    public ContinuationChunkerSequencingThread(
            ContinuationChunker continuationChunker,
            BlockingQueue<Pattern> patternsQueue,
            Map<Pattern, BlockingQueue<QueueItem>> aggregatingQueues,
            Path absoluteCountedDir,
            Path absoluteChunkedDir,
            Path continuationCountedDir,
            Path continuationChunkedDir,
            long readerMemory,
            int updateInterval,
            Status status) {
        this.continuationChunker = continuationChunker;
        this.patternsQueue = patternsQueue;
        this.aggregatingQueues = aggregatingQueues;
        this.absoluteCountedDir = absoluteCountedDir;
        this.absoluteChunkedDir = absoluteChunkedDir;
        this.continuationCountedDir = continuationCountedDir;
        this.continuationChunkedDir = continuationChunkedDir;
        this.readerMemory = readerMemory;
        this.updateInterval = updateInterval;
        this.status = status;
    }

    @Override
    public void run() {
        try {
            while (!patternsQueue.isEmpty()) {
                Pattern pattern = patternsQueue.poll();
                if (pattern == null) {
                    continue;
                }

                boolean chunked = false;
                Path input;
                boolean isAbsolute = pattern.isAbsolute();
                if (isAbsolute && status.getCounted(false).contains(pattern)) {
                    input = absoluteCountedDir;
                } else if (isAbsolute
                        && status.getChunkedPatterns(false).contains(pattern)) {
                    input = absoluteChunkedDir;
                    chunked = true;
                } else if (!isAbsolute
                        && status.getCounted(true).contains(pattern)) {
                    input = continuationCountedDir;
                } else if (!isAbsolute
                        && status.getChunkedPatterns(true).contains(pattern)) {
                    input = continuationChunkedDir;
                    chunked = true;
                } else {
                    LOGGER.trace("Pattern '{}' not available.", pattern);
                    StatisticalNumberHelper.count("pattern not available");
                    // wait for aggregators to finish pattern
                    Thread.sleep(SLEEP_TIME);
                    patternsQueue.offer(pattern);
                    continue;
                }
                input = input.resolve(pattern.toString());

                if (!chunked) {
                    sequence(pattern, input);
                } else {
                    try (DirectoryStream<Path> inputDirStream =
                            Files.newDirectoryStream(input)) {
                        for (Path inputFile : inputDirStream) {
                            sequence(pattern, inputFile);
                        }
                    }
                }
            }

            continuationChunker.sequencingIsDone();
            LOGGER.debug("{} finished.",
                    ContinuationChunkerSequencingThread.class.getSimpleName());
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void sequence(Pattern pattern, Path inputFile) throws IOException,
    InterruptedException {
        LOGGER.debug("Sequencing '{}' from '{}'.", pattern, inputFile);
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Files.newInputStream(inputFile)), (int) readerMemory)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.splitAtChar(line, '\t');
                QueueItem item =
                        new QueueItem(pattern, split.get(0), Long.valueOf(split
                                .get(1)));
                while (!aggregatingQueues.get(pattern).offer(item,
                        QUEUE_WAIT_TIME, TimeUnit.SECONDS)) {
                    LOGGER.trace("{} idle, because aggregating queue full.",
                            ContinuationChunkerSequencingThread.class
                            .getSimpleName());
                    StatisticalNumberHelper.count("Idle "
                            + ContinuationChunkerSequencingThread.class
                            .getSimpleName()
                            + " because aggregatingQueue full");
                }

                // To get memory average of ReadQueueItem. Don't forget to:
                // - add import
                // - uncomment classmexer.jar in pom
                // - add javaagent in run.sh to MAVEN_OPTS.
                StatisticalNumberHelper.average("QueueItem", MemoryUtil
                        .deepMemoryUsageOf(item, VisibilityFilter.ALL));
            }
            QueueItem item = new QueueItem(pattern, null, 0);
            while (!aggregatingQueues.get(pattern).offer(item, QUEUE_WAIT_TIME,
                    TimeUnit.SECONDS)) {
                LOGGER.trace("{} idle, because aggregating queue full.",
                        ContinuationChunkerSequencingThread.class
                        .getSimpleName());
                StatisticalNumberHelper.count("Idle "
                        + ContinuationChunkerSequencingThread.class
                        .getSimpleName()
                        + " because aggregatingQueue full");
            }
        }
    }
}
