package de.glmtk.counting.absolute;

import gnu.trove.iterator.TObjectLongIterator;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.counting.absolute.Chunker.QueueItem;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;

public class ChunkerAggregatingThread implements Runnable {

    @SuppressWarnings("unused")
    private static final long AVERAGE_SEQUENCE_SIZE = 15;

    private static final long QUEUE_WAIT_TIME = 10;

    private static final int TAB_COUNT_NL_BYTES = "\t10\n".getBytes().length;

    private static final Logger LOGGER = LogManager
            .getLogger(ChunkerAggregatingThread.class);

    private static class Chunk {

        public int size = 0;

        public int counter = 0;

        public TObjectLongMap<String> sequenceCounts =
                new TObjectLongHashMap<String>();

    }

    private Chunker chunker;

    private BlockingQueue<QueueItem> queue;

    private Path outputDir;

    private long chunkSize;

    private Status status;

    private Map<Pattern, Chunk> chunks;

    private Map<Pattern, Queue<Path>> chunkFiles;

    public ChunkerAggregatingThread(
            Chunker chunker,
            BlockingQueue<QueueItem> queue,
            Path outputDir,
            long chunkSize,
            Status status) {
        this.chunker = chunker;
        this.queue = queue;
        this.outputDir = outputDir;
        this.chunkSize = chunkSize;
        this.status = status;

        chunks = new HashMap<Pattern, Chunk>();
        chunkFiles = new HashMap<Pattern, Queue<Path>>();
    }

    @Override
    public void run() {
        try {
            while (!(chunker.isSequencingDone() && queue.isEmpty())) {
                QueueItem item =
                        queue.poll(QUEUE_WAIT_TIME, TimeUnit.MILLISECONDS);
                if (item == null) {
                    LOGGER.trace("ChunkerAggregatingThread idle, because queue empty.");
                    StatisticalNumberHelper
                            .count("Idle ChunkerAggregatingThread because queue empty");
                    continue;
                }

                Chunk chunk = chunks.get(item.pattern);
                if (chunk == null) {
                    chunk = new Chunk();
                    chunks.put(item.pattern, chunk);
                    chunkFiles.put(item.pattern, new LinkedList<Path>());
                }

                if (chunk.sequenceCounts.adjustOrPutValue(item.sequence, 1, 1) == 1) {
                    chunk.size +=
                            item.sequence.getBytes().length
                                    + TAB_COUNT_NL_BYTES;
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

            status.getAbsoluteChunked().putAll(chunkFiles);
            status.writeStatusToFile();
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("ChunkerAggregatingThread finished.");
    }

    private void writeChunkToFile(Pattern pattern, Chunk chunk)
            throws IOException {
        Path file =
                outputDir.resolve(pattern.toString()).resolve(
                        "chunk" + chunk.counter);
        Files.deleteIfExists(file);
        try (OutputStreamWriter writer =
                new OutputStreamWriter(Files.newOutputStream((file)))) {
            TObjectLongIterator<String> it = chunk.sequenceCounts.iterator();
            for (int i = 0; i != chunk.sequenceCounts.size(); ++i) {
                it.advance();
                writer.write(it.key());
                writer.write('\t');
                writer.write(((Long) it.value()).toString());
                writer.write('\n');
            }
        }
        chunkFiles.get(pattern).add(file.getFileName());
    }

}
