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

import de.glmtk.Status;
import de.glmtk.counting.AbsoluteChunker.QueueItem;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;

// TODO: find better estimate for map size, to make chunks more memory
// effecient.
/* package */class AbsoluteChunkerAggregatingThread implements Runnable {

    @SuppressWarnings("unused")
    private static final long AVERAGE_SEQUENCE_SIZE = 15;

    private static final long QUEUE_WAIT_TIME = 10;

    private static final int TAB_COUNT_NL_BYTES = "\t10\n".getBytes().length;

    private static final Logger LOGGER = LogManager
            .getLogger(AbsoluteChunkerAggregatingThread.class);

    private static class Chunk {

        public int size = 0;

        public int counter = 0;

        public Map<String, Long> sequenceCounts = new HashMap<String, Long>();

    }

    private AbsoluteChunker absoluteChunker;

    private BlockingQueue<QueueItem> queue;

    private Path outputDir;

    private long chunkSize;

    private Status status;

    private Map<Pattern, Chunk> chunks;

    private Map<Pattern, List<Path>> chunkFiles;

    public AbsoluteChunkerAggregatingThread(
            AbsoluteChunker absoluteChunker,
            BlockingQueue<QueueItem> queue,
            Path outputDir,
            long chunkSize,
            Status status) {
        this.absoluteChunker = absoluteChunker;
        this.queue = queue;
        this.outputDir = outputDir;
        this.chunkSize = chunkSize;
        this.status = status;

        chunks = new HashMap<Pattern, Chunk>();
        chunkFiles = new HashMap<Pattern, List<Path>>();
    }

    @Override
    public void run() {
        try {
            while (!(absoluteChunker.isSequencingDone() && queue.isEmpty())) {
                QueueItem item =
                        queue.poll(QUEUE_WAIT_TIME, TimeUnit.MILLISECONDS);
                if (item == null) {
                    LOGGER.trace("AbsoluteChunkerAggregatingThread idle, because queue empty.");
                    StatisticalNumberHelper
                    .count("Idle AbsoluteChunkerAggregatingThread because queue empty");
                    continue;
                }

                Chunk chunk = chunks.get(item.pattern);
                if (chunk == null) {
                    chunk = new Chunk();
                    chunks.put(item.pattern, chunk);
                    chunkFiles.put(item.pattern, new LinkedList<Path>());
                }

                Long count = chunk.sequenceCounts.get(item.sequence);
                if (count == null) {
                    chunk.size +=
                            item.sequence.getBytes().length
                                    + TAB_COUNT_NL_BYTES;
                    chunk.sequenceCounts.put(item.sequence, 1L);
                } else {
                    chunk.sequenceCounts.put(item.sequence, count + 1L);
                }

                if (chunk.size > chunkSize) {
                    writeChunkToFile(item.pattern, chunk);
                    int counter = chunk.counter + 1;
                    chunk = new Chunk();
                    chunk.counter = counter;
                    chunks.put(item.pattern, chunk);
                }
            }

            for (Map.Entry<Pattern, Chunk> entry : chunks.entrySet()) {
                writeChunkToFile(entry.getKey(), entry.getValue());
            }

            status.addChunked(false, chunkFiles);
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("AbsoluteChunkerAggregatingThread finished.");
    }

    private void writeChunkToFile(Pattern pattern, Chunk chunk)
            throws IOException {
        Path file =
                outputDir.resolve(pattern.toString()).resolve(
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
    }

}
