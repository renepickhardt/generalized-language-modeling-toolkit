package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.common.Counter;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.util.NioUtils;
import de.glmtk.util.ThreadUtils;

public enum NGramTimesCounter {
    NGRAM_TIMES_COUNTER;

    private static final Logger LOGGER = LogManager.getFormatterLogger(NGramTimesCounter.class);

    private class Thread implements Callable<Object> {
        private Pattern pattern;
        private long[] nGramTimes;

        @Override
        public Object call() throws InterruptedException, IOException {
            while (!patternQueue.isEmpty()) {
                pattern = patternQueue.poll(Constants.QUEUE_TIMEOUT,
                        TimeUnit.MILLISECONDS);
                if (pattern == null)
                    continue;

                LOGGER.debug("Counting pattern '%s'.", pattern);

                countNGramTimes();
                nGramTimesForPattern.put(pattern, nGramTimes);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void countNGramTimes() throws IOException {
            nGramTimes = new long[4];
            nGramTimes[0] = 0L;
            nGramTimes[1] = 0L;
            nGramTimes[2] = 0L;
            nGramTimes[3] = 0L;
            Counter counter = new Counter();

            Path inputDir = pattern.isAbsolute()
                    ? absoluteDir
                            : continuationDir;
            Path inputFile = inputDir.resolve(pattern.toString());
            int memory = (int) Math.min(Files.size(inputFile), readerMemory);
            try (BufferedReader reader = NioUtils.newBufferedReader(inputFile,
                    Constants.CHARSET, memory)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Counter.getSequenceAndCounter(line, counter);

                    long count = counter.getOnePlusCount();
                    if (count == 0 || count > 4)
                        continue;
                    ++nGramTimes[(int) (count - 1)];
                }
            }
        }
    }

    private Progress progress;
    private Path outputFile;
    private Path absoluteDir;
    private Path continuationDir;
    private BlockingQueue<Pattern> patternQueue;
    private ConcurrentHashMap<Pattern, long[]> nGramTimesForPattern;
    private int readerMemory;

    public void count(Status status,
                      Path outputFile,
                      Path absoluteDir,
                      Path continuationDir) throws Exception {
        OUTPUT.setPhase(Phase.NGRAM_TIMES_COUNTING);

        if (status.isNGramTimesCounted()) {
            LOGGER.debug("Status reports ngram times already counted, returning.");
            return;
        }

        Set<Pattern> patterns = status.getCounted();

        this.outputFile = outputFile;
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        patternQueue = new LinkedBlockingQueue<>(patterns);
        nGramTimesForPattern = new ConcurrentHashMap<>();
        progress = new Progress(patternQueue.size());
        calculateMemory();

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != CONFIG.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        ThreadUtils.executeThreads(CONFIG.getNumberOfThreads(), threads);

        Glmtk.validateExpectedResults("ngram times couting", patterns,
                nGramTimesForPattern.keySet());

        writeToFile();
        status.setNGramTimesCounted();
    }

    private void calculateMemory() {
        readerMemory = CONFIG.getReaderMemory();
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
    }

    private void writeToFile() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile,
                Constants.CHARSET)) {
            for (Entry<Pattern, long[]> entry : nGramTimesForPattern.entrySet()) {
                Pattern pattern = entry.getKey();
                long[] nGramTimes = entry.getValue();

                writer.append(pattern.toString()).append('\t');
                writer.append(Long.toString(nGramTimes[0])).append('\t');
                writer.append(Long.toString(nGramTimes[1])).append('\t');
                writer.append(Long.toString(nGramTimes[2])).append('\t');
                writer.append(Long.toString(nGramTimes[3])).append('\n');
            }
        }
    }
}
