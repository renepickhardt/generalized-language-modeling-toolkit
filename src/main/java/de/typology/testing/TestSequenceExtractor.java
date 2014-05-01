package de.typology.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.typology.indexing.WordIndex;
import de.typology.patterns.PatternBuilder;
import de.typology.patterns.PatternTransformer;

/**
 * This class extracts all sequences that are needed for computing the
 * Kneser-Ney smoothed values for a set of given test sequences.
 * 
 * @author Martin Koerner
 * 
 */
public class TestSequenceExtractor {

    private File testSequenceFile;

    private File absoluteDirectory;

    private File continuationDirectory;

    private File outputDirectory;

    private String delimiter;

    @SuppressWarnings("unused")
    private WordIndex wordIndex;

    public TestSequenceExtractor(
            File testSequenceFile,
            File absoluteDirectory,
            File continuationDirectory,
            File outputDirectory,
            String delimiter,
            WordIndex wordIndex) {
        this.testSequenceFile = testSequenceFile;
        this.absoluteDirectory = absoluteDirectory;
        this.continuationDirectory = continuationDirectory;
        this.outputDirectory = outputDirectory;
        this.delimiter = delimiter;
        this.wordIndex = wordIndex;

    }

    public void extractSequences(int maxModelLength, int cores) {

        // read test sequences into HashSet
        ArrayList<String> sequences = new ArrayList<String>();
        try {
            BufferedReader testSequenceReader =
                    new BufferedReader(new FileReader(testSequenceFile));
            String line;
            while ((line = testSequenceReader.readLine()) != null) {
                sequences.add(line);
            }
            testSequenceReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<boolean[]> absolutePatterns =
                PatternBuilder.getGLMForSmoothingPatterns(maxModelLength);

        // call SequenceExtractorTasks

        // initialize executerService
        // int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(cores);

        for (boolean[] absolutePattern : absolutePatterns) {
            // extract absolute sequences
            String absoluteStringPattern =
                    PatternTransformer.getStringPattern(absolutePattern);
            File absoluteworkingDirectory =
                    new File(absoluteDirectory.getAbsolutePath() + "/"
                            + absoluteStringPattern);
            File absoluteOutputDirectory =
                    new File(outputDirectory + "/"
                            + absoluteDirectory.getName() + "/"
                            + absoluteStringPattern);
            SequenceExtractorTask absoluteSET =
                    new SequenceExtractorTask(sequences, absolutePattern,
                            absoluteworkingDirectory, absoluteOutputDirectory,
                            delimiter);
            executorService.execute(absoluteSET);

        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void extractContinuationSequences(int maxModelLength, int cores) {

        // read test sequences into HashSet
        ArrayList<String> sequences = new ArrayList<String>();
        try {
            BufferedReader testSequenceReader =
                    new BufferedReader(new FileReader(testSequenceFile));
            String line;
            while ((line = testSequenceReader.readLine()) != null) {
                sequences.add(line);
            }
            testSequenceReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // call SequenceExtractorTasks

        // initialize executerService
        // int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(cores);

        for (File continuationTypeDirectory : continuationDirectory.listFiles()) {
            // extract absolute sequences
            String continuationStringPattern =
                    continuationTypeDirectory.getName();
            boolean[] continuationPattern =
                    PatternTransformer
                            .getBooleanPattern(continuationStringPattern
                                    .replaceAll("_", "0"));
            File continuationOutputDirectory =
                    new File(outputDirectory + "/"
                            + continuationDirectory.getName() + "/"
                            + continuationStringPattern);
            SequenceExtractorTask continuationSET =
                    new SequenceExtractorTask(sequences, continuationPattern,
                            continuationTypeDirectory,
                            continuationOutputDirectory, delimiter);
            executorService.execute(continuationSET);

        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    // public void extractContinuationSequences(int maxModelLength, int cores) {
    // ArrayList<boolean[]> absolutePatterns = PatternBuilder
    // .getLMPatterns(maxModelLength);
    //
    // // initialize executerService
    // // int cores = Runtime.getRuntime().availableProcessors();
    // ExecutorService executorService = Executors.newFixedThreadPool(cores);
    // for (boolean[] absolutePattern : absolutePatterns) {
    // File originalSequencesDirectory = new File(
    // this.outputDirectory.getAbsolutePath()
    // + "/"
    // + this.absoluteDirectory.getName()
    // + "/"
    // + PatternTransformer
    // .getStringPattern(absolutePattern));
    // File outputDirectory = new File(
    // this.outputDirectory.getAbsolutePath() + "/continuation");
    // ContinuationExtractorTask cet = new ContinuationExtractorTask(
    // originalSequencesDirectory, absolutePattern,
    // this.absoluteDirectory, outputDirectory, this.wordIndex,
    // this.delimiter);
    // executorService.execute(cet);
    // }
    //
    // executorService.shutdown();
    // try {
    // executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    // } catch (InterruptedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    //
    // }
}
