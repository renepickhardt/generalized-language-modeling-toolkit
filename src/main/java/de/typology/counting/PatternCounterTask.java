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

import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;
import de.typology.sequencing.Sequencer;

/**
 * A class for running {@link Sequencer} and {@link Aggregator} for a given
 * pattern.
 */
public class PatternCounterTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    private InputStream input;

    private Path outputDir;

    @SuppressWarnings("unused")
    private WordIndex wordIndex;

    @SuppressWarnings("unused")
    private Pattern pattern;

    private String delimiter;

    @SuppressWarnings("unused")
    private String beforeLine;

    @SuppressWarnings("unused")
    private String afterLine;

    private boolean isContinuation;

    private boolean deleteTempFiles;

    /**
     * Expects an {@code input} where each line contains a number of words
     * separated by white space. Extract the counts of all distinct sequences
     * described by {@code pattern} and writes them to <em>indexed files</em> in
     * {@code outputDir}.
     * 
     * @param input
     *            {@link InputStream} to be read.
     * @param outputDir
     *            Dir where <em>indexed files</em> should be written to.
     * @param wordIndex
     *            {@link WordIndex} of the corpus.
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
            Path outputDir,
            WordIndex wordIndex,
            Pattern pattern,
            String delimiter,
            String beforeLine,
            String afterLine,
            boolean isContinuation,
            boolean deleteTempFiles) throws IOException {
        this.input = input;
        this.outputDir = outputDir;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.delimiter = delimiter;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.isContinuation = isContinuation;

        Files.createDirectory(outputDir);
    }

    @Override
    public void run() {
        try {
            // SEQUENCING //////////////////////////////////////////////////////

            Path sequencerOutputDir =
                    outputDir.getParent().resolve(
                            outputDir.getFileName() + "-split");
            Files.createDirectory(sequencerOutputDir);

            logger.info("sequencing:  " + sequencerOutputDir);

            //            Sequencer sequencer =
            //                    new Sequencer(input, sequencerOutputDir, wordIndex,
            //                            pattern, beforeLine, afterLine, isContinuation,
            //                            true, delimiter);
            //            sequencer.splitIntoFiles();
            input.close();

            // AGGREGATING /////////////////////////////////////////////////////

            logger.info("aggregating: " + outputDir);

            try (DirectoryStream<Path> sequencerOutputFiles =
                    Files.newDirectoryStream(sequencerOutputDir)) {
                for (Path sequencerOutputFile : sequencerOutputFiles) {
                    try (InputStream input =
                            Files.newInputStream(sequencerOutputFile);
                            OutputStream output =
                                    Files.newOutputStream(outputDir
                                            .resolve(sequencerOutputFile
                                                    .getFileName()))) {
                        Aggregator aggregator =
                                new Aggregator(input, output, delimiter,
                                        isContinuation);
                        aggregator.aggregate();
                    }
                }
            }

            // delete sequencerOutputDir
            if (deleteTempFiles) {
                // TODO: replace by non legacy file api
                FileUtils.deleteDirectory(sequencerOutputDir.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
