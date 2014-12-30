package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.common.Output;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.util.NioUtils;

public class Merger {

    private static final int AVAILABLE_MEMORY_PERCENT = 40;

    private static final int NUM_PARALLEL_READERS = 10;

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Merger.class);

    private boolean continuation;

    private Progress progress;

    /* package */Merger(
            boolean continuation) {
        this.continuation = continuation;
    }

    public void merge(
            Status status,
            Set<Pattern> patterns,
            Path chunkedDir,
            Path countedDir) throws IOException {
        if (!continuation) {
            OUTPUT.setPhase(Phase.ABSOLUTE_MERGING, true);
        } else {
            OUTPUT.setPhase(Phase.CONTINUATION_MERGING, true);
        }
        progress = new Progress(patterns.size());

        if (patterns.isEmpty()) {
            LOGGER.debug("No chunks to merge, returning.");
            progress.set(1.0);
            return;
        }
        LOGGER.debug("patterns = %s", patterns);
        Files.createDirectories(countedDir);

        // Calculate Memory ////////////////////////////////////////////////////
        LOGGER.debug("Calculating Memory...");
        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMemory =
                (AVAILABLE_MEMORY_PERCENT * totalFreeMemory) / 100;
        long readerMemory = availableMemory / CONFIG.getNumberOfCores() / 2;
        long writerMemory = availableMemory / CONFIG.getNumberOfCores() / 2;

        LOGGER.debug("totalFreeMemory = %s",
                Output.humanReadableByteCount(totalFreeMemory, false));
        LOGGER.debug("availableMemory = %s",
                Output.humanReadableByteCount(availableMemory, false));
        LOGGER.debug("readerMemory    = %s",
                Output.humanReadableByteCount(readerMemory, false));
        LOGGER.debug("writerMemory    = %s",
                Output.humanReadableByteCount(writerMemory, false));

        // Prepare Threads /////////////////////////////////////////////////////
        LOGGER.debug("Preparing Threads...");
        BlockingQueue<Pattern> queue =
                new LinkedBlockingDeque<Pattern>(patterns);
        List<MergerThread> mergerThreads = new LinkedList<MergerThread>();
        for (int i = 0; i != CONFIG.getNumberOfCores(); ++i) {
            mergerThreads.add(new MergerThread(this, status, queue, chunkedDir,
                    countedDir, readerMemory, writerMemory,
                    NUM_PARALLEL_READERS, continuation));
        }

        // Launch Threads //////////////////////////////////////////////////////
        LOGGER.debug("Launching Threads...");
        try {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(CONFIG.getNumberOfCores());

            for (MergerThread mergerThread : mergerThreads) {
                executorService.execute(mergerThread);
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        if (NioUtils.isDirEmpty(chunkedDir)) {
            Files.deleteIfExists(chunkedDir);
        }
    }

    /* package */void increaseProgress() {
        synchronized (progress) {
            progress.increase(1);
        }
    }

}
