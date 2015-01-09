package de.glmtk.languagemodels;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.util.ThreadUtils;

public class AlphaCalculator {
    private static final Logger LOGGER = LogManager.getFormatterLogger(AlphaCalculator.class);

    private class Thread implements Callable<Object> {
        private Pattern pattern;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                pattern = patternQueue.poll(Constants.QUEUE_TIMEOUT,
                        TimeUnit.MILLISECONDS);
                if (pattern == null)
                    continue;

                LOGGER.debug("Calculating pattern '%s'.", pattern);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }
    }

    private Config config;

    private Progress progress;
    private Status status;
    private Path absoluteDir;
    private Path continuationDir;
    private Path alphaDir;
    private BlockingQueue<Pattern> patternQueue;
    private int readerMemory;
    private int writerMemory;

    public AlphaCalculator(Config config) {
        this.config = config;
    }

    public void calculateAlphas(Status status,
                                Set<Pattern> patterns,
                                Path absoluteDir,
                                Path continuationDir,
                                Path alphaDir) throws Exception {
        OUTPUT.setPhase(Phase.CALCULATING_ALPHAS);

        LOGGER.debug("patterns = %s", patterns);
        if (patterns.isEmpty())
            return;

        Files.createDirectories(alphaDir);

        this.status = status;
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        this.alphaDir = alphaDir;
        patternQueue = new LinkedBlockingQueue<>(patterns);
        calculateMemory();

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        progress = OUTPUT.newProgress(patterns.size());
        ThreadUtils.executeThreads(config.getNumberOfThreads(), threads);
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();

        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }
}
