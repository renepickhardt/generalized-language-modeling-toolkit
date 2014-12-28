package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.common.Pattern;
import de.glmtk.counting.ContinuationChunker.QueueItem;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;

/* package */class ContinuationChunkerSequencingThread implements Runnable {

    private static final long QUEUE_IDLE_TIME = Constants.QUEUE_IDLE_TIME;

    private static final int SLEEP_TIME = 10;

    private static final String CLASS_NAME =
            ContinuationChunkerSequencingThread.class.getSimpleName();

    private static final Logger LOGGER = LogManager
            .getLogger(ContinuationChunkerSequencingThread.class);

    private ContinuationChunker continuationChunker;

    private Status status;

    private BlockingQueue<Pattern> patternsQueue;

    private Map<Pattern, BlockingQueue<QueueItem>> aggregatingQueues;

    private Path absoluteCountedDir;

    private Path absoluteChunkedDir;

    private Path continuationCountedDir;

    private Path continuationChunkedDir;

    private long readerMemory;

    public ContinuationChunkerSequencingThread(
            ContinuationChunker continuationChunker,
            Status status,
            BlockingQueue<Pattern> patternsQueue,
            Map<Pattern, BlockingQueue<QueueItem>> aggregatingQueues,
            Path absoluteCountedDir,
            Path absoluteChunkedDir,
            Path continuationCountedDir,
            Path continuationChunkedDir,
            long readerMemory) {
        this.continuationChunker = continuationChunker;
        this.status = status;
        this.patternsQueue = patternsQueue;
        this.aggregatingQueues = aggregatingQueues;
        this.absoluteCountedDir = absoluteCountedDir;
        this.absoluteChunkedDir = absoluteChunkedDir;
        this.continuationCountedDir = continuationCountedDir;
        this.continuationChunkedDir = continuationChunkedDir;
        this.readerMemory = readerMemory;
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
                boolean fromAbsolute = false;
                Path input;
                boolean isAbsolute = pattern.isAbsolute();
                if (isAbsolute && status.getCounted(false).contains(pattern)) {
                    input = absoluteCountedDir;
                    fromAbsolute = true;
                } else if (isAbsolute
                        && status.getChunkedPatterns(false).contains(pattern)) {
                    input = absoluteChunkedDir;
                    chunked = true;
                    fromAbsolute = true;
                } else if (!isAbsolute
                        && status.getCounted(true).contains(pattern)) {
                    input = continuationCountedDir;
                } else if (!isAbsolute
                        && status.getChunkedPatterns(true).contains(pattern)) {
                    input = continuationChunkedDir;
                    chunked = true;
                } else {
                    LOGGER.trace("Pattern '{}' not available.", pattern);
                    StatisticalNumberHelper.count("Pattern not available");
                    // wait for aggregators to finish pattern
                    Thread.sleep(SLEEP_TIME);
                    patternsQueue.offer(pattern);
                    continue;
                }
                input = input.resolve(pattern.toString());

                if (!chunked) {
                    sequence(pattern, input, true, fromAbsolute);
                } else {
                    try (DirectoryStream<Path> inputDirStream =
                            Files.newDirectoryStream(input)) {
                        Iterator<Path> i = inputDirStream.iterator();
                        while (i.hasNext()) {
                            sequence(pattern, i.next(), !i.hasNext(),
                                    fromAbsolute);
                        }
                    }
                }
            }

            continuationChunker.sequencingThreadDone();
            LOGGER.debug("Done.");
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void sequence(
            Pattern pattern,
            Path inputFile,
            boolean lastFile,
            boolean fromAbsolute) throws IOException, InterruptedException {
        LOGGER.debug("Sequencing '{}' from '{}'.", pattern, inputFile);
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Files.newInputStream(inputFile)), (int) readerMemory)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> split = StringUtils.splitAtChar(line, '\t');
                if (split.size() < 2) {
                    LOGGER.error(
                            "Unable to derminine sequence and counts of line '{}'.",
                            line);
                    continue;
                }

                String sequence = split.get(0);
                long count = fromAbsolute ? 1 : Long.valueOf(split.get(1));
                QueueItem item = new QueueItem(pattern, sequence, count);

                while (!aggregatingQueues.get(pattern).offer(item,
                        QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS)) {
                    LOGGER.trace("Idle, because queue full.");
                    StatisticalNumberHelper.count(CLASS_NAME
                            + " idle (queue full)");
                }

                if (Constants.DEBUG_AVERAGE_MEMORY) {
                    StatisticalNumberHelper.average(
                            "ContinuationChunker.QueueItem Memory", MemoryUtil
                                    .deepMemoryUsageOf(item,
                                            VisibilityFilter.ALL));
                }
            }
            if (lastFile) {
                QueueItem item = new QueueItem(pattern, null, 0);
                while (!aggregatingQueues.get(pattern).offer(item,
                        QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS)) {
                    LOGGER.trace("Idle, because queue full.");
                    StatisticalNumberHelper.count(CLASS_NAME
                            + " idle (queue full)");
                }
            }
        }
    }
}
