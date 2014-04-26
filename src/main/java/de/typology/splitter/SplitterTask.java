package de.typology.splitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;

/**
 * A class for running Sequencer and Aggregator for a given pattern.
 * 
 * @author Martin Koerner
 * 
 */
public class SplitterTask implements Runnable {

    private InputStream inputStream;

    private File outputDirectory;

    private WordIndex wordIndex;

    private boolean[] pattern;

    private String patternLabel;

    private String delimiter;

    private int startSortAtColumn;

    private boolean deleteTempFiles;

    private String addBeforeSentence;

    private String addAfterSentence;

    private boolean sequenceModifyCounts;

    private boolean aggregateCompleteLine;

    private boolean additionalCounts;

    private Logger logger = LogManager.getLogger(this.getClass().getName());

    public SplitterTask(
            InputStream inputStream,
            File outputDirectory,
            WordIndex wordIndex,
            boolean[] pattern,
            String patternLabel,
            String delimiter,
            int startSortAtColumn,
            boolean deleteTempFiles,
            String addBeforeSentence,
            String addAfterSentence,
            boolean sequenceModifyCounts,
            boolean aggregateCompleteLine,
            boolean additionalCounts) {
        this.inputStream = inputStream;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.patternLabel = patternLabel;
        this.delimiter = delimiter;
        this.startSortAtColumn = startSortAtColumn;
        this.deleteTempFiles = deleteTempFiles;
        this.addBeforeSentence = addBeforeSentence;
        this.addAfterSentence = addAfterSentence;
        this.sequenceModifyCounts = sequenceModifyCounts;
        this.aggregateCompleteLine = aggregateCompleteLine;
        this.additionalCounts = additionalCounts;
    }

    @Override
    public void run() {
        try {
            File sequencerOutputDirectory =
                    new File(outputDirectory.getAbsolutePath() + "/"
                            + patternLabel + "-split");
            if (sequencerOutputDirectory.exists()) {
                FileUtils.deleteDirectory(sequencerOutputDirectory);
            }
            sequencerOutputDirectory.mkdir();
            logger.info("start building: "
                    + sequencerOutputDirectory.getAbsolutePath());

            // initialize sequencer
            Sequencer sequencer =
                    new Sequencer(inputStream, sequencerOutputDirectory,
                            wordIndex, pattern, addBeforeSentence,
                            addAfterSentence, delimiter, sequenceModifyCounts,
                            startSortAtColumn);
            sequencer.splitIntoFiles();

            File aggregatedOutputDirectory =
                    new File(outputDirectory.getAbsolutePath() + "/"
                            + patternLabel);
            if (aggregatedOutputDirectory.exists()) {
                FileUtils.deleteDirectory(aggregatedOutputDirectory);
            }
            aggregatedOutputDirectory.mkdir();
            logger.info("aggregate into: " + aggregatedOutputDirectory);

            for (File splitFile : sequencerOutputDirectory.listFiles()) {
                Aggregator aggregator =
                        new Aggregator(splitFile, new File(
                                aggregatedOutputDirectory.getAbsolutePath()
                                        + "/" + splitFile.getName()),
                                delimiter, startSortAtColumn, additionalCounts);
                if (aggregateCompleteLine) {
                    aggregator.aggregateWithoutCounts();
                } else {
                    aggregator.aggregateCounts();
                }
            }

            // delete sequencerOutputDirectory
            if (deleteTempFiles) {
                FileUtils.deleteDirectory(sequencerOutputDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
