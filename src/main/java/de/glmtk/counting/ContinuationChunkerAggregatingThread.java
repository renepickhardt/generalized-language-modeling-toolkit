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

import de.glmtk.Counter;
import de.glmtk.Status;
import de.glmtk.counting.ContinuationChunker.QueueItem;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

/* package */class ContinuationChunkerAggregatingThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 10;

    private static final int TAB_COUNTER_NL_BYTES = "\t10\t10\t10\t10\n"
            .getBytes().length;

    private static final Logger LOGGER = LogManager
            .getLogger(ContinuationChunkerAggregatingThread.class);

    private static class Chunk {

        public int size = 0;

        public int counter = 0;

        public Map<String, Counter> sequenceCounts =
                new HashMap<String, Counter>();

    }

    private ContinuationChunker continuationChunker;

    private BlockingQueue<QueueItem> queue;

    private Map<Pattern, List<Pattern>> sourceToPattern;

    private Path continuationChunkedDir;

    private long chunkSize;

    private Status status;

    Map<Pattern, Chunk> chunks;

    Map<Pattern, List<Path>> chunkFiles;

    public ContinuationChunkerAggregatingThread(
            ContinuationChunker continuationChunker,
            BlockingQueue<QueueItem> queue,
            Map<Pattern, List<Pattern>> sourceToPattern,
            Path continuationChunkedDir,
            long chunkSize,
            Status status) {
        this.continuationChunker = continuationChunker;
        this.queue = queue;
        this.sourceToPattern = sourceToPattern;
        this.continuationChunkedDir = continuationChunkedDir;
        this.chunkSize = chunkSize;
        this.status = status;

        chunks = new HashMap<Pattern, Chunk>();
        chunkFiles = new HashMap<Pattern, List<Path>>();
    }

    @Override
    public void run() {
        try {
            while (!(continuationChunker.isSequencingDone() && queue.isEmpty())) {
                QueueItem item =
                        queue.poll(QUEUE_WAIT_TIME, TimeUnit.MILLISECONDS);
                if (item == null) {
                    LOGGER.trace("{} idle, because queue empty.",
                            ContinuationChunkerAggregatingThread.class
                                    .getSimpleName());
                    StatisticalNumberHelper.count("Idle "
                            + ContinuationChunkerAggregatingThread.class
                                    .getSimpleName() + " because queue empty");
                    continue;
                }

                for (Pattern pattern : sourceToPattern.get(item.pattern)) {
                    Chunk chunk = chunks.get(pattern);
                    if (chunk == null) {
                        chunk = new Chunk();
                        chunks.put(pattern, chunk);
                        chunkFiles.put(pattern, new LinkedList<Path>());
                    }

                    if (item.sequence == null) {
                        LOGGER.debug("Finishing pattern '{}'.", pattern);
                        writeChunkToFile(pattern, chunk);
                        status.addChunked(true, pattern,
                                chunkFiles.get(pattern));
                        continue;
                    }

                    String sequence =
                            pattern.apply(StringUtils.splitAtChar(
                                    item.sequence, ' ').toArray(new String[0]));
                    Counter counter = chunk.sequenceCounts.get(sequence);
                    if (counter == null) {
                        counter = new Counter();
                        chunk.size +=
                                sequence.getBytes().length
                                + TAB_COUNTER_NL_BYTES;
                        chunk.sequenceCounts.put(sequence, counter);
                    }
                    counter.add(item.count);

                    if (chunk.size > chunkSize) {
                        writeChunkToFile(pattern, chunk);
                        int chunkCounter = chunk.counter + 1;
                        chunk = new Chunk();
                        chunk.counter = chunkCounter;
                        chunks.put(pattern, chunk);
                    }
                    LOGGER.debug("Added count for '{}' from '{}'.", pattern,
                            item.pattern);
                }
            }

            for (Map.Entry<Pattern, Chunk> entry : chunks.entrySet()) {
                LOGGER.debug("{}, {}", entry.getKey(), entry.getValue());
                //writeChunkToFile(entry.getKey(), entry.getValue());
            }

            LOGGER.debug("{} finished",
                    ContinuationChunkerAggregatingThread.class.getSimpleName());
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
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
    }

}
