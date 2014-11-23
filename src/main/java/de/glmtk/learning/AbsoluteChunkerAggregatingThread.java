package de.glmtk.learning;

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
import de.glmtk.learning.AbsoluteChunker.QueueItem;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;

// TODO: find better estimate for map size, to make chunks more memory
// effecient.
/* package */class AbsoluteChunkerAggregatingThread implements Runnable {

    private static final long QUEUE_IDLE_TIME = Constants.QUEUE_IDLE_TIME;

    private static final int TAB_COUNT_NL_BYTES = "\t10\n".getBytes().length;

    @SuppressWarnings("unused")
    private static final long AVERAGE_SEQUENCE_SIZE = 15;

    private static final String CLASS_NAME =
            AbsoluteChunkerAggregatingThread.class.getSimpleName();

    private static final Logger LOGGER = LogManager
            .getLogger(AbsoluteChunkerAggregatingThread.class);

    private static class Chunk {

        public int size = 0;

        public int counter = 0;

        public Map<String, Long> sequenceCounts = new HashMap<String, Long>();

    }

    private AbsoluteChunker absoluteChunker;

    private Status status;

    private BlockingQueue<QueueItem> queue;

    private Path absoluteChunkedDir;

    private long chunkSize;

    private Map<Pattern, Chunk> chunks;

    private Map<Pattern, List<Path>> chunkFiles;

    public AbsoluteChunkerAggregatingThread(
            AbsoluteChunker absoluteChunker,
            Status status,
            BlockingQueue<QueueItem> queue,
            Path absoluteChunkedDir,
            long chunkSize) {
        this.absoluteChunker = absoluteChunker;
        this.status = status;
        this.queue = queue;
        this.absoluteChunkedDir = absoluteChunkedDir;
        this.chunkSize = chunkSize;

        chunks = new HashMap<Pattern, Chunk>();
        chunkFiles = new HashMap<Pattern, List<Path>>();
    }

    @Override
    public void run() {
        try {
            while (!(absoluteChunker.isSequencingDone() && queue.isEmpty())) {
                QueueItem item =
                        queue.poll(QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS);
                if (item == null) {
                    LOGGER.trace("Idle, because queue empty.");
                    StatisticalNumberHelper.count(CLASS_NAME
                            + " idle (queue empty)");
                    continue;
                }

                addToChunk(item.pattern, item.sequence);
            }

            for (Map.Entry<Pattern, Chunk> entry : chunks.entrySet()) {
                Pattern pattern = entry.getKey();
                Chunk chunk = entry.getValue();
                writeChunkToFile(pattern, chunk);
                LOGGER.debug("Finished pattern '{}'.", pattern);
            }

            status.addChunked(false, chunkFiles);
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("Done.");
    }

    private void addToChunk(Pattern pattern, String sequence)
            throws IOException {
        Chunk chunk = chunks.get(pattern);
        if (chunk == null) {
            chunk = new Chunk();
            chunks.put(pattern, chunk);
            chunkFiles.put(pattern, new LinkedList<Path>());
        }

        Long count = chunk.sequenceCounts.get(sequence);
        if (count == null) {
            chunk.size += sequence.getBytes().length + TAB_COUNT_NL_BYTES;
            chunk.sequenceCounts.put(sequence, 1L);
        } else {
            chunk.sequenceCounts.put(sequence, count + 1L);
        }

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
                absoluteChunkedDir.resolve(pattern.toString()).resolve(
                        "chunk" + chunk.counter);
        Files.deleteIfExists(file);
        try (OutputStreamWriter writer =
                new OutputStreamWriter(Files.newOutputStream((file)))) {
            Map<String, Long> sortedSequenceCounts =
                    new TreeMap<String, Long>(chunk.sequenceCounts);
            for (Map.Entry<String, Long> entry : sortedSequenceCounts
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
