package de.typology.filtering;

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
import de.typology.indexing.WordIndex;

public class FiltererTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    private InputStream input;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private boolean[] pattern;

    private String beforeLine;

    private String afterLine;

    private boolean deleteTempFiles;

    public FiltererTask(
            InputStream input,
            Path outputDirectory,
            WordIndex wordIndex,
            boolean[] pattern,
            String beforeLine,
            String afterLine,
            boolean deleteTempFiles) throws IOException {
        this.input = input;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
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
            //Files.createDirectory(sequencerOutputDirectory);

            logger.info("sequencing:  " + sequencerOutputDirectory);

            Sequencer sequencer =
                    new Sequencer(input, sequencerOutputDirectory, wordIndex,
                            pattern, beforeLine, afterLine, false, false, null);
            sequencer.splitIntoFiles();
            input.close();

            // REDUCING ////////////////////////////////////////////////////////

            logger.info("reducing:    " + outputDirectory);

            try (DirectoryStream<Path> sequencerOutputFiles =
                    Files.newDirectoryStream(sequencerOutputDirectory)) {
                for (Path sequencerOutputFile : sequencerOutputFiles) {
                    try (InputStream input =
                            Files.newInputStream(sequencerOutputFile);
                            OutputStream output =
                                    Files.newOutputStream(outputDirectory
                                            .resolve(sequencerOutputFile
                                                    .getFileName()))) {
                        Reducer reducer = new Reducer(input, output);
                        reducer.reduce();
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
