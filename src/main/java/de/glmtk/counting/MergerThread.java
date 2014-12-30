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

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.common.Counter;
import de.glmtk.common.Pattern;
import de.glmtk.counting.SequenceCountReader.SequencerCountReaderComparator;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;

/* package */class MergerThread implements Runnable {

    private static final long QUEUE_IDLE_TIME = Constants.QUEUE_IDLE_TIME;

    private static final String CLASS_NAME = MergerThread.class.getSimpleName();

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(MergerThread.class);

    @SuppressWarnings("unused")
    private Merger merger;

    private Status status;

    private BlockingQueue<Pattern> queue;

    private Path chunkedDir;

    private Path countedDir;

    private long readerMemory;

    private long writerMemory;

    private int numParallelReaders;

    private boolean continuation;

    public MergerThread(
            Merger merger,
            Status status,
            BlockingQueue<Pattern> queue,
            Path chunkedDir,
            Path countedDir,
            long readerMemory,
            long writerMemory,
            int numParallelReaders,
            boolean continuation) {
        this.merger = merger;
        this.status = status;
        this.queue = queue;
        this.chunkedDir = chunkedDir;
        this.countedDir = countedDir;
        this.readerMemory = readerMemory;
        this.writerMemory = writerMemory;
        this.numParallelReaders = numParallelReaders;
        this.continuation = continuation;
    }

    @Override
    public void run() {
        try {
            while (!queue.isEmpty()) {
                Pattern pattern =
                        queue.poll(QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    LOGGER.debug("Idle.");
                    StatisticalNumberHelper.count(CLASS_NAME + " idle");
                    continue;
                }

                Path patternDir = chunkedDir.resolve(pattern.toString());

                int mergeCounter = 0;
                List<Path> chunks, curChunks = null;
                while ((chunks = status.getChunks(continuation, pattern))
                        .size() != 1) {
                    curChunks =
                            new LinkedList<Path>(
                                    chunks.subList(
                                            0,
                                            Math.min(numParallelReaders,
                                                    chunks.size())));

                    Path mergeFile = Paths.get("merge" + mergeCounter);
                    LOGGER.debug("Merging pattern %s:\t%s -> %s.", pattern,
                            curChunks, mergeFile);
                    mergeChunksToFile(patternDir, curChunks, mergeFile);
                    status.performChunkedMerge(continuation, pattern,
                            curChunks, mergeFile);

                    for (Path chunk : curChunks) {
                        Files.delete(patternDir.resolve(chunk));
                    }

                    ++mergeCounter;
                }

                Path src = patternDir.resolve(chunks.get(0));
                Path dest = countedDir.resolve(pattern.toString());
                LOGGER.debug("Finishing pattern %s:\t%s\t-> %s.", pattern, src,
                        dest);
                Files.deleteIfExists(dest);
                Files.move(src, dest);

                status.finishChunkedMerge(continuation, pattern);

                if (NioUtils.isDirEmpty(patternDir)) {
                    Files.delete(patternDir);
                }
            }
        } catch (InterruptedException | IOException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.debug("Done");
    }

    private void mergeChunksToFile(
            Path patternDir,
            List<Path> curChunks,
            Path mergeFile) throws IOException {
        Files.deleteIfExists(patternDir.resolve(mergeFile));

        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(
                        Files.newOutputStream(patternDir.resolve(mergeFile)),
                        Constants.CHARSET), (int) writerMemory)) {
            PriorityQueue<SequenceCountReader> readerQueue =
                    new PriorityQueue<SequenceCountReader>(numParallelReaders,
                            new SequencerCountReaderComparator());
            for (Path chunk : curChunks) {
                readerQueue.add(new SequenceCountReader(new BufferedReader(
                        new InputStreamReader(Files.newInputStream(patternDir
                                .resolve(chunk)), Constants.CHARSET),
                                (int) readerMemory / numParallelReaders)));
            }

            String sequence = null;
            Counter counter = null;
            while (!readerQueue.isEmpty()) {
                SequenceCountReader reader = readerQueue.poll();
                if (reader.isEmpty()) {
                    reader.close();
                    continue;
                }

                String s = reader.getSequence();
                Counter c = reader.getCounter();

                if (s.equals(sequence)) {
                    counter.add(c);
                } else {
                    if (sequence != null) {
                        writer.write(sequence);
                        writer.write('\t');
                        if (continuation) {
                            writer.write(counter.toString());
                        } else {
                            writer.write(Long.toString(counter
                                    .getOnePlusCount()));
                        }
                        writer.write('\n');
                    }
                    sequence = s;
                    counter = c;
                }
                reader.nextLine();
                readerQueue.add(reader);
            }
        }
    }

}
