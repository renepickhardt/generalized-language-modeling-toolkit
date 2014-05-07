package de.typology.counting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.patterns.Pattern;

/**
 * Counts absolute counts of sequences for a number of patterns.
 */
public class AbsoluteCounter {

    private static Logger logger = LogManager.getLogger();

    private Path inputDir;

    private Path outputDir;

    private String delimiter;

    private int numberOfCores;

    public AbsoluteCounter(
            Path inputDir,
            Path outputDir,
            String delimiter,
            int numberOfCores) throws IOException {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.delimiter = delimiter;
        this.numberOfCores = numberOfCores;

        Files.createDirectory(outputDir);
    }

    public void split(List<Pattern> patterns) throws IOException,
            InterruptedException {
        logger.info("Counting absolute counts of sequences.");

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        try (DirectoryStream<Path> patternDirs =
                Files.newDirectoryStream(inputDir)) {
            for (Path patternDir : patternDirs) {
                Path patternOutputDir =
                        outputDir.resolve(patternDir.getFileName());
                Files.createDirectory(patternOutputDir);

                try (DirectoryStream<Path> patternFiles =
                        Files.newDirectoryStream(patternDir)) {
                    for (Path patternFile : patternFiles) {
                        Path patternOutputFile =
                                patternOutputDir.resolve(patternFile
                                        .getFileName());
                        InputStream input = Files.newInputStream(patternFile);
                        OutputStream output =
                                Files.newOutputStream(patternOutputFile);
                        executorService.execute(new AbsoluteCounterTask(input,
                                output, delimiter));
                    }
                }
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}
