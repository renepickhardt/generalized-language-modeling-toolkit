package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Counter;
import de.glmtk.Status;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StatisticalNumberHelper;

/* package */class MergerThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 10;

    private static final Logger LOGGER = LogManager
            .getLogger(MergerThread.class);

    @SuppressWarnings("unused")
    private Merger merger;

    private BlockingQueue<Pattern> patternQueue;

    private Path inputDir;

    private Path outputDir;

    private long readerMemory;

    private long writerMemory;

    @SuppressWarnings("unused")
    private int updateInterval;

    private int numParallelReaders;

    private Status status;

    public MergerThread(
            Merger merger,
            BlockingQueue<Pattern> patternQueue,
            Path inputDir,
            Path outputDir,
            long readerMemory,
            long writerMemory,
            int updateInterval,
            int numParallelReaders,
            Status status) {
        this.merger = merger;
        this.patternQueue = patternQueue;
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.readerMemory = readerMemory;
        this.writerMemory = writerMemory;
        this.updateInterval = updateInterval;
        this.numParallelReaders = numParallelReaders;
        this.status = status;
    }

    @Override
    public void run() {
        try {
            while (!patternQueue.isEmpty()) {
                Pattern pattern =
                        patternQueue.poll(QUEUE_WAIT_TIME,
                                TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    LOGGER.debug("MergerThread idle.");
                    StatisticalNumberHelper.count("MergerThread idle");
                    continue;
                }

                Path patternDir = inputDir.resolve(pattern.toString());

                Queue<Path> chunks = status.getAbsoluteChunked().get(pattern);
                for (int mergeCounter = 0; chunks.size() != 1; ++mergeCounter) {
                    Queue<Path> curChunks = new LinkedList<Path>();
                    for (int i = 0; i != numParallelReaders
                            && !chunks.isEmpty(); ++i) {
                        curChunks.add(chunks.poll());
                    }

                    Path mergeFile = Paths.get("merge" + mergeCounter);

                    LOGGER.debug("Merging pattern {}: {} -> {}.", pattern,
                            curChunks, mergeFile);
                    mergeChunksToFile(patternDir, curChunks,
                            patternDir.resolve(mergeFile));

                    chunks.add(mergeFile);
                    status.writeStatusToFile();
                }

                Path src = patternDir.resolve(chunks.poll());
                Path dest = outputDir.resolve(pattern.toString());
                LOGGER.debug("Finishing pattern {}: {} -> {}.", pattern, src,
                        dest);
                Files.deleteIfExists(dest);
                Files.move(src, dest);

                status.getAbsoluteChunked().remove(pattern);
                status.getAbsoluteCounted().add(pattern);
                status.writeStatusToFile();
            }
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("MergerThread finished.");
    }

    private void mergeChunksToFile(
            Path patternDir,
            Queue<Path> curChunks,
            Path mergeFile) throws IOException {
        Files.deleteIfExists(mergeFile);
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(
                        Files.newOutputStream(mergeFile)), (int) writerMemory)) {
            PriorityQueue<SequencerCountReader> readerQueue =
                    new PriorityQueue<SequencerCountReader>(numParallelReaders,
                            SequencerCountReader.COMPARATOR);
            for (Path chunk : curChunks) {
                readerQueue.add(new SequencerCountReader(new BufferedReader(
                        new InputStreamReader(Files.newInputStream(patternDir
                                .resolve(chunk))), (int) readerMemory
                                / numParallelReaders)));
            }

            String sequence = null;
            Counter counter = null;
            while (!readerQueue.isEmpty()) {
                SequencerCountReader reader = readerQueue.poll();
                String s = reader.getSequence();
                Counter c = reader.getCounter();

                if (s.equals(sequence)) {
                    counter.add(counter);
                } else {
                    if (sequence != null) {
                        writer.write(sequence);
                        writer.write('\t');
                        writer.write(Long.toString(counter.getOnePlusCount()));
                        writer.write('\n');
                    }
                    sequence = s;
                    counter = c;
                }
                reader.nextLine();

                if (reader.isEmpty()) {
                    reader.close();
                } else {
                    readerQueue.add(reader);
                }
            }
        }
    }
}
