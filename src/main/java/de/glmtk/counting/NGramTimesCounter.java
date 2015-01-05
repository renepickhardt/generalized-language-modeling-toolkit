package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.Counter;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.util.NioUtils;

public enum NGramTimesCounter {
    NGRAM_TIMES_COUNTER;

    private static final Logger LOGGER = LogManager.getFormatterLogger(NGramTimesCounter.class);

    private Progress progress;
    private Path outputFile;
    private Path absoluteDir;
    private Path continuationDir;
    private Set<Pattern> patterns;
    private int readerMemory;

    public void count(Status status,
                      Path outputFile,
                      Path absoluteDir,
                      Path continuationDir) throws IOException {
        OUTPUT.setPhase(Phase.NGRAM_TIMES_COUNTING);

        if (status.isNGramTimesCounted()) {
            LOGGER.debug("Status reports ngram times already counted, returning.");
            return;
        }

        this.outputFile = outputFile;
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        patterns = status.getCounted();
        progress = new Progress(patterns.size());
        calculateMemory();

        countNGramTimes();

        status.setNGramTimesCounted();
    }

    private void calculateMemory() {
        readerMemory = CONFIG.getReaderMemory();
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
    }

    private void countNGramTimes() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile,
                Constants.CHARSET)) {
            for (Pattern pattern : patterns) {
                long[] nGramTimes = {0L, 0L, 0L, 0L};
                Counter counter = new Counter();

                Path inputDir = pattern.isAbsolute()
                        ? absoluteDir
                        : continuationDir;
                Path inputFile = inputDir.resolve(pattern.toString());
                try (BufferedReader reader = NioUtils.newBufferedReader(
                        inputFile, Constants.CHARSET, readerMemory)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Counter.getSequenceAndCounter(line, counter);

                        long count = counter.getOnePlusCount();
                        if (count == 0 || count > 4)
                            continue;
                        ++nGramTimes[(int) (count - 1)];
                    }
                }

                writer.write(pattern.toString());
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[0]));
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[1]));
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[2]));
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[3]));
                writer.write('\n');

                progress.increase(1);
            }
        }
    }
}
