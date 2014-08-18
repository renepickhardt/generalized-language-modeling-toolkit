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
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Counter;
import de.glmtk.Status;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.NioUtils;
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

                int mergeCounter = 0;
                List<Path> chunks, curChunks = null;
                while ((chunks = status.getChunks(false, pattern)).size() != 1) {
                    curChunks =
                            new LinkedList<Path>(
                                    chunks.subList(
                                            0,
                                            Math.min(numParallelReaders,
                                                    chunks.size())));

                    Path mergeFile = Paths.get("merge" + mergeCounter);
                    LOGGER.debug("Merging pattern {}: {} -> {}.", pattern,
                            curChunks, mergeFile);
                    mergeChunksToFile(patternDir, curChunks, mergeFile);
                    status.performChunkedMerge(false, pattern, curChunks,
                            mergeFile);

                    for (Path chunk : curChunks) {
                        Files.delete(patternDir.resolve(chunk));
                    }

                    ++mergeCounter;
                }

                Path src = patternDir.resolve(chunks.get(0));
                Path dest = outputDir.resolve(pattern.toString());
                LOGGER.debug("Finishing pattern {}: {} -> {}.", pattern, src,
                        dest);
                Files.deleteIfExists(dest);
                Files.move(src, dest);

                status.finishChunkedMerge(false, pattern);

                if (NioUtils.isDirEmpty(patternDir)) {
                    Files.delete(patternDir);
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("{} finished.", Merger.class.getSimpleName());
    }

    private void mergeChunksToFile(
            Path patternDir,
            List<Path> curChunks,
            Path mergeFile) throws IOException {
        Files.deleteIfExists(patternDir.resolve(mergeFile));

        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(
                        Files.newOutputStream(patternDir.resolve(mergeFile))),
                        (int) writerMemory)) {
            PriorityQueue<SequencerCountReader> readerQueue =
                    new PriorityQueue<SequencerCountReader>(numParallelReaders);
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
