package de.typology.counting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.Sequencer;
import de.typology.filtering.Filter;
import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;

/**
 * A class for running {@link Sequencer} and {@link Aggregator} for a given
 * pattern.
 */
public class PatternCounterTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    private InputStream input;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private Filter filter;

    private Pattern pattern;

    private String delimiter;

    private String beforeLine;

    private String afterLine;

    private boolean isContinuation;

    private boolean deleteTempFiles;

    /**
     * Expects an {@code input} where each line contains a number of words
     * separated by white space. Extract the counts of all distinct sequences
     * described by {@code pattern} and writes them to <em>indexed files</em> in
     * {@code outputDirectory}.
     * 
     * @param input
     *            {@link InputStream} to be read.
     * @param outputDirectory
     *            Directory where <em>indexed files</em> should be written to.
     * @param wordIndex
     *            {@link WordIndex} of the corpus.
     * @param filter
     *            Filter of the testing samples.
     * @param pattern
     *            Pattern specifying sequences.
     * @param delimiter
     *            Delimiter that separates Sequences and Counts in output.
     * @param beforeLine
     *            Prepended before each line before sequencing.
     * @param afterLine
     *            Appended after each line before sequencing.
     * @param isContinuation
     *            Whether we are calculating this for continuation or absolute
     *            counts. Absolute only has 1+ Counts (Continuation has 1+, 1,
     *            2, 3+), Absolute counts completely, Continuation uses absolute
     *            counts as input.
     * @param deleteTempFiles
     *            Whether temporary created files should be deleted (
     *            {@link Sequencer} output).
     */
    public PatternCounterTask(
            InputStream input,
            Path outputDirectory,
            WordIndex wordIndex,
            Filter filter,
            Pattern pattern,
            String delimiter,
            String beforeLine,
            String afterLine,
            boolean isContinuation,
            boolean deleteTempFiles) throws IOException {
        this.input = input;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.filter = filter;
        this.pattern = pattern;
        this.delimiter = delimiter;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.isContinuation = isContinuation;
        this.deleteTempFiles = deleteTempFiles;

        Files.createDirectory(outputDirectory);
    }

    @Override
    public void run() {
        try {
            // SEQUENCING //////////////////////////////////////////////////////

            Path sequencerOutputDirectory =
                    outputDirectory.getParent().resolve(
                            outputDirectory.getFileName() + "-split");
            Files.createDirectory(sequencerOutputDirectory);

            logger.info("sequencing:  " + sequencerOutputDirectory);

            Sequencer sequencer =
                    new Sequencer(input, sequencerOutputDirectory, wordIndex,
                            filter, pattern, beforeLine, afterLine,
                            isContinuation, true, delimiter);
            sequencer.splitIntoFiles();
            input.close();

            // AGGREGATING /////////////////////////////////////////////////////

            logger.info("aggregating: " + outputDirectory);

            try (DirectoryStream<Path> sequencerOutputFiles =
                    Files.newDirectoryStream(sequencerOutputDirectory)) {
                for (Path sequencerOutputFile : sequencerOutputFiles) {
                    try (InputStream input =
                            Files.newInputStream(sequencerOutputFile);
                            OutputStream output =
                                    Files.newOutputStream(outputDirectory
                                            .resolve(sequencerOutputFile
                                                    .getFileName()))) {
                        Aggregator aggregator =
                                new Aggregator(input, output, delimiter,
                                        isContinuation);
                        aggregator.aggregate();
                    }
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
