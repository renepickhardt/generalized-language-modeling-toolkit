package de.typology.splitter;

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

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;

public class AbsoluteSplitter {

    private InputStream input;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private String delimiter;

    private String beforeLine;

    private String afterLine;

    private int numberOfCores;

    private boolean deleteTempFiles;

    private Logger logger = LogManager.getLogger(this.getClass().getName());

    public AbsoluteSplitter(
            InputStream input,
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

    public void split(List<boolean[]> patterns) throws IOException,
            InterruptedException {
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        for (boolean[] pattern : patterns) {
            logger.debug("execute SplitterTask for: "
                    + PatternTransformer.getStringPattern(pattern)
                    + " sequences");

            SplitterTask splitterTask =
                    new SplitterTask(input,
                            outputDirectory.resolve(PatternTransformer
                                    .getStringPattern(pattern)), wordIndex,
                            pattern, delimiter, beforeLine, afterLine, false,
                            deleteTempFiles);
            executorService.execute(splitterTask);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }
}
