package de.glmtk.counting.legacy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.pattern.Pattern;

public class AbsoluteCounterChunkThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 10;

    private static final Logger LOGGER = LogManager
            .getLogger(AbsoluteCounterChunkThread.class);

    @SuppressWarnings("unused")
    private AbsoluteCounter absoluteCounter;

    private BlockingQueue<Pattern> patternQueue;

    private Map<Pattern, Queue<Path>> chunkedQueues;

    private Path inputDir;

    private Path outputDir;

    private long chunkReaderMemory;

    private long chunkWriterMemory;

    private long chunkMemorySize;

    @SuppressWarnings("unused")
    private int updateInterval;

    public AbsoluteCounterChunkThread(
            AbsoluteCounter absoluteCounter,
            BlockingQueue<Pattern> patternQueue,
            Map<Pattern, Queue<Path>> chunkedQueues,
            Path inputDir,
            Path outputDir,
            long chunkReaderMemory,
            long chunkWriterMemory,
            long chunkMemorySize,
            int updateInterval) {
        this.absoluteCounter = absoluteCounter;
        this.patternQueue = patternQueue;
        this.chunkedQueues = chunkedQueues;
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.chunkReaderMemory = chunkReaderMemory;
        this.chunkWriterMemory = chunkWriterMemory;
        this.chunkMemorySize = chunkMemorySize;
        this.updateInterval = updateInterval;
    }

    @Override
    public void run() {
        try {
            while (!patternQueue.isEmpty()) {
                Pattern pattern =
                        patternQueue.poll(QUEUE_WAIT_TIME,
                                TimeUnit.MILLISECONDS);
                if (pattern != null) {
                    LOGGER.debug("Chunking pattern: " + pattern);
                    chunk(pattern);
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("AbsoluteCounterChunkThread finished.");
    }

    private void chunk(Pattern pattern) throws IOException {
        Path tmpDir = outputDir.resolve(pattern + ".tmp");
        Files.createDirectories(tmpDir);

        int chunkCounter = 0;
        long chunkSize = 0;
        Map<String, Long> sequenceCounts = new TreeMap<String, Long>();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        Files.newInputStream(inputDir.resolve(pattern
                                .toString()))), (int) chunkReaderMemory)) {
            String sequence;
            while ((sequence = reader.readLine()) != null) {
                if (chunkSize > chunkMemorySize) {
                    writeChunkToFile(pattern, sequenceCounts,
                            tmpDir.resolve("chunk" + chunkCounter));

                    ++chunkCounter;
                    chunkSize = 0;
                    sequenceCounts = new TreeMap<String, Long>();
                }

                Long count = sequenceCounts.get(sequence);
                if (count == null) {
                    chunkSize += sequence.getBytes().length;
                    sequenceCounts.put(sequence, 1L);
                } else {
                    sequenceCounts.put(sequence, count + 1);
                }
            }

            writeChunkToFile(pattern, sequenceCounts,
                    tmpDir.resolve("chunk" + chunkCounter));
        }

        LOGGER.trace("Chunked pattern {} -> {}.", pattern,
                chunkedQueues.get(pattern));
    }

    private void writeChunkToFile(
            Pattern pattern,
            Map<String, Long> sequenceCounts,
            Path file) throws IOException {
        Files.deleteIfExists(file);
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(
                        Files.newOutputStream(file)), (int) chunkWriterMemory)) {
            for (Map.Entry<String, Long> sequenceCount : sequenceCounts
                    .entrySet()) {
                String sequence = sequenceCount.getKey();
                Long count = sequenceCount.getValue();
                writer.write(sequence);
                writer.write('\t');
                writer.write(count.toString());
                writer.write('\n');
            }
        }
        chunkedQueues.get(pattern).add(file);
    }

}
