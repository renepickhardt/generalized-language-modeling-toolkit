package de.typology.splitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;
import de.typology.patterns.PatternTransformer;

/**
 * Split
 * 
 * @author Martin Koerner
 * 
 */
public class AbsoluteSplitter {

    private File trainingFile;

    private File indexFile;

    private File outputDirectory;

    private String delimiter;

    private boolean deleteTempFiles;

    private String addBeforeSentence;

    private String addAfterSentence;

    private Logger logger = LogManager.getLogger(this.getClass().getName());

    public AbsoluteSplitter(
            File trainingFile,
            File indexFile,
            File absoluteDirectory,
            String delimiter,
            boolean deleteTempFiles,
            String addBeforeSentence,
            String addAfterSentence) {
        this.trainingFile = trainingFile;
        this.indexFile = indexFile;
        outputDirectory = absoluteDirectory;
        this.delimiter = delimiter;
        this.deleteTempFiles = deleteTempFiles;
        this.addBeforeSentence = addBeforeSentence;
        this.addAfterSentence = addAfterSentence;
        // delete old directory
        if (absoluteDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(absoluteDirectory);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        absoluteDirectory.mkdir();
    }

    public void split(List<boolean[]> patterns, int cores) throws IOException,
            InterruptedException {
        logger.info("read word index: " + indexFile.getAbsolutePath());
        WordIndex wordIndex = new WordIndex(indexFile);

        // initialize executerService
        ExecutorService executorService = Executors.newFixedThreadPool(cores);

        for (boolean[] pattern : patterns) {
            logger.debug("execute SplitterTask for: "
                    + PatternTransformer.getStringPattern(pattern)
                    + " sequences");

            try {
                InputStream trainingFileInputStream =
                        new FileInputStream(trainingFile);
                SplitterTask splitterTask =
                        new SplitterTask(trainingFileInputStream,
                                outputDirectory, wordIndex, pattern,
                                PatternTransformer.getStringPattern(pattern),
                                delimiter, deleteTempFiles, addBeforeSentence,
                                addAfterSentence, false);
                executorService.execute(splitterTask);
            } catch (FileNotFoundException e) {
                logger.error("trainingFile not found: "
                        + trainingFile.getAbsolutePath());
                throw e;
            }
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Interrupted
            throw e;
        }
    }
}
