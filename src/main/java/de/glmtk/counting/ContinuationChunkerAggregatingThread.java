package de.glmtk.counting;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.counting.ContinuationChunker.QueueItem;
import de.glmtk.utils.Counter;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

/* package */class ContinuationChunkerAggregatingThread implements Runnable {

    private static final long QUEUE_IDLE_TIME = Constants.QUEUE_IDLE_TIME;

    private static final int TAB_COUNTER_NL_BYTES = "\t10\t10\t10\t10\n"
            .getBytes().length;

    private static final String CLASS_NAME =
            ContinuationChunkerAggregatingThread.class.getSimpleName();

    private static final Logger LOGGER = LogManager
            .getLogger(ContinuationChunkerAggregatingThread.class);

    private static class Chunk {

        public int size = 0;

        public int counter = 0;

        public Map<String, Counter> sequenceCounts =
                new HashMap<String, Counter>();

    }

    private ContinuationChunker continuationChunker;

    private Status status;

    private BlockingQueue<QueueItem> queue;

    private Map<Pattern, List<Pattern>> sourceToPattern;

    private Path continuationChunkedDir;

    private long chunkSize;

    private Map<Pattern, Chunk> chunks;

    private Map<Pattern, List<Path>> chunkFiles;

    public ContinuationChunkerAggregatingThread(
            ContinuationChunker continuationChunker,
            Status status,
            BlockingQueue<QueueItem> queue,
            Map<Pattern, List<Pattern>> sourceToPattern,
            Path continuationChunkedDir,
            long chunkSize) {
        this.continuationChunker = continuationChunker;
        this.status = status;
        this.queue = queue;
        this.sourceToPattern = sourceToPattern;
        this.continuationChunkedDir = continuationChunkedDir;
        this.chunkSize = chunkSize;

        chunks = new HashMap<Pattern, Chunk>();
        chunkFiles = new HashMap<Pattern, List<Path>>();
    }

    @Override
    public void run() {
        try {
            while (!(continuationChunker.isSequencingDone() && queue.isEmpty())) {
                QueueItem item =
                        queue.poll(QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS);
                if (item == null) {
                    LOGGER.trace("Idle, because queue empty.");
                    StatisticalNumberHelper.count(CLASS_NAME
                            + " idle (queue empty)");
                    continue;
                }

                for (Pattern pattern : sourceToPattern.get(item.pattern)) {
                    addToChunk(pattern, item.sequence, item.count);
                }
            }

            LOGGER.debug("Done.");
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addToChunk(Pattern pattern, String sequence, long count)
            throws IOException {
        Chunk chunk = chunks.get(pattern);
        if (chunk == null) {
            chunk = new Chunk();
            chunks.put(pattern, chunk);
            chunkFiles.put(pattern, new LinkedList<Path>());
        }

        if (sequence == null) {
            writeChunkToFile(pattern, chunk);
            status.addChunked(true, pattern,
                    new LinkedList<Path>(chunkFiles.get(pattern)));
            LOGGER.debug("Finished pattern '{}'.", pattern);
            return;
        }

        String seq =
                pattern.apply(StringUtils.splitAtChar(sequence, ' ').toArray(
                        new String[0]));
        Counter counter = chunk.sequenceCounts.get(seq);
        if (counter == null) {
            counter = new Counter();
            chunk.size += seq.getBytes().length + TAB_COUNTER_NL_BYTES;
            chunk.sequenceCounts.put(seq, counter);
        }
        counter.add(count);

        if (chunk.size > chunkSize) {
            writeChunkToFile(pattern, chunk);
            int chunkCounter = chunk.counter + 1;
            chunk = new Chunk();
            chunk.counter = chunkCounter;
            chunks.put(pattern, chunk);
        }
    }

    private void writeChunkToFile(Pattern pattern, Chunk chunk)
            throws IOException {
        Path file =
                continuationChunkedDir.resolve(pattern.toString()).resolve(
                        "chunk" + chunk.counter);
        Files.deleteIfExists(file);
        try (OutputStreamWriter writer =
                new OutputStreamWriter(Files.newOutputStream(file))) {
            Map<String, Counter> sortedSequenceCounts =
                    new TreeMap<String, Counter>(chunk.sequenceCounts);
            for (Map.Entry<String, Counter> entry : sortedSequenceCounts
                    .entrySet()) {
                writer.write(entry.getKey());
                writer.write('\t');
                writer.write(entry.getValue().toString());
                writer.write('\n');
            }
        }
        chunkFiles.get(pattern).add(file.getFileName());
        LOGGER.debug("Wrote chunk for pattern '{}': '{}'.", pattern, file);
    }

}
