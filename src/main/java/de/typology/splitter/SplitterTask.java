package de.typology.splitter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;

/**
 * A class for running {@link Sequencer} and {@link Aggregator} for a given
 * pattern.
 * 
 * @author Martin Koerner
 * 
 */
public class SplitterTask implements Runnable {

    private InputStream input;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private boolean[] pattern;

    private String patternLabel;

    private String delimiter;

    private boolean deleteTempFiles;

    private String beforeLine;

    private String afterLine;

    private boolean isSmoothing;

    private Logger logger = LogManager.getLogger(this.getClass().getName());

    public SplitterTask(
            InputStream input,
            Path outputDirectory,
            WordIndex wordIndex,
            boolean[] pattern,
            String patternLabel,
            String delimiter,
            boolean deleteTempFiles,
            String beforeLine,
            String afterLine,
            boolean isSmoothing) {
        this.input = input;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.patternLabel = patternLabel;
        this.delimiter = delimiter;
        this.deleteTempFiles = deleteTempFiles;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.isSmoothing = isSmoothing;
    }

    @Override
    public void run() {
        try {
            // SEQUENCING //////////////////////////////////////////////////////
            Path sequencerOutputDirectory =
                    outputDirectory.resolve(patternLabel + "-split");
            Files.createDirectory(sequencerOutputDirectory);
            logger.info("start building: " + sequencerOutputDirectory);

            Sequencer sequencer =
                    new Sequencer(input, sequencerOutputDirectory, wordIndex,
                            pattern, beforeLine, afterLine, delimiter,
                            isSmoothing);
            sequencer.splitIntoFiles();

            // AGGREGATING /////////////////////////////////////////////////////

            Path aggregatorOutputDirectory =
                    outputDirectory.resolve(patternLabel);
            Files.createDirectory(aggregatorOutputDirectory);
            logger.info("aggregate into: " + aggregatorOutputDirectory);

            try (DirectoryStream<Path> sequencerOutputContents =
                    Files.newDirectoryStream(sequencerOutputDirectory)) {
                for (Path file : sequencerOutputContents) {
                    InputStream input = new FileInputStream(file.toString());
                    OutputStream output =
                            new FileOutputStream(aggregatorOutputDirectory
                                    .resolve(file.getFileName()).toString());

                    Aggregator aggregator =
                            new Aggregator(input, output, delimiter,
                                    isSmoothing);
                    aggregator.aggregate();
                }
            }

            // delete sequencerOutputDirectory
            if (deleteTempFiles) {
                // TODO: replace by non legacy file api
                FileUtils.deleteDirectory(sequencerOutputDirectory.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
