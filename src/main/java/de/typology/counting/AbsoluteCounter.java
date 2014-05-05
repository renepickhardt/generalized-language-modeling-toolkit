package de.typology.counting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;

/**
 * Counts absolute counts of sequences for a number of patterns.
 */
public class AbsoluteCounter {

    private static Logger logger = LogManager.getLogger();

    private Path input;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private String delimiter;

    private String beforeLine;

    private String afterLine;

    private int numberOfCores;

    private boolean deleteTempFiles;

    public AbsoluteCounter(
            Path input,
            Path outputDirectory,
            WordIndex wordIndex,
            String delimiter,
            String beforeLine,
            String afterLine,
            int numberOfCores,
            boolean deleteTempFiles) throws IOException {
        this.input = input;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.delimiter = delimiter;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.numberOfCores = numberOfCores;
        this.deleteTempFiles = deleteTempFiles;

        Files.createDirectory(outputDirectory);
    }

    public void split(List<Pattern> patterns) throws IOException,
            InterruptedException {
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        for (Pattern pattern : patterns) {
            logger.info("calculate absolute counts for " + pattern);

            // Need to create a new InputStream for each iteration, as
            // SplitterTask will read complete stream on each pass.
            InputStream inputStream = Files.newInputStream(input);

            PatternCounterTask patternCounterTask =
                    new PatternCounterTask(inputStream,
                            outputDirectory.resolve(pattern.toString()),
                            wordIndex, pattern, delimiter, beforeLine,
                            afterLine, false, deleteTempFiles);
            executorService.execute(patternCounterTask);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }
}
