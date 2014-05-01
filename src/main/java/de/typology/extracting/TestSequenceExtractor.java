package de.typology.extracting;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.typology.patterns.PatternBuilder;
import de.typology.patterns.PatternTransformer;

/**
 * This class extracts all sequences that are needed for computing the
 * Kneser-Ney smoothed values for a set of given test sequences.
 */
public class TestSequenceExtractor {

    private Path testSequenceFile;

    private Path absoluteDirectory;

    private Path continuationDirectory;

    private Path outputDirectory;

    private String delimiter;

    private int modelLength;

    private int numberOfCores;

    public TestSequenceExtractor(
            Path testSequenceFile,
            Path absoluteDirectory,
            Path continuationDirectory,
            Path outputDirectory,
            String delimiter,
            int modelLength,
            int numberOfCores) throws IOException {
        this.testSequenceFile = testSequenceFile;
        this.absoluteDirectory = absoluteDirectory;
        this.continuationDirectory = continuationDirectory;
        this.outputDirectory = outputDirectory;
        this.delimiter = delimiter;
        this.modelLength = modelLength;
        this.numberOfCores = numberOfCores;

        Files.createDirectory(outputDirectory);
    }

    public void extractSequences() {
        // read test sequences into HashSet
        ArrayList<String> sequences = new ArrayList<String>();
        try {
            BufferedReader testSequenceReader =
                    Files.newBufferedReader(testSequenceFile,
                            Charset.defaultCharset());
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
                PatternBuilder.getGLMForSmoothingPatterns(modelLength);

        // call SequenceExtractorTasks

        // initialize executerService
        // int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        for (boolean[] absolutePattern : absolutePatterns) {
            // extract absolute sequences
            String absoluteStringPattern =
                    PatternTransformer.getStringPattern(absolutePattern);
            Path absoluteworkingDirectory =
                    absoluteDirectory.resolve(absoluteStringPattern);
            Path absoluteOutputDirectory =
                    outputDirectory.resolve(absoluteDirectory.getFileName())
                            .resolve(absoluteStringPattern);
            SequenceExtractorTask absoluteSET =
                    new SequenceExtractorTask(sequences, absolutePattern,
                            absoluteworkingDirectory.toFile(),
                            absoluteOutputDirectory.toFile(), delimiter);
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

    public void extractContinuationSequences() throws IOException {
        // read test sequences into HashSet
        ArrayList<String> sequences = new ArrayList<String>();
        try {
            BufferedReader testSequenceReader =
                    Files.newBufferedReader(testSequenceFile,
                            Charset.defaultCharset());
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
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        try (DirectoryStream<Path> continuationFiles =
                Files.newDirectoryStream(continuationDirectory)) {
            for (Path continuationTypeDirectory : continuationFiles) {
                // extract absolute sequences
                String continuationStringPattern =
                        continuationTypeDirectory.getFileName().toString();
                boolean[] continuationPattern =
                        PatternTransformer
                                .getBooleanPattern(continuationStringPattern
                                        .replaceAll("_", "0"));
                Path continuationOutputDirectory =
                        outputDirectory.resolve(
                                continuationDirectory.getFileName()).resolve(
                                continuationStringPattern);
                SequenceExtractorTask continuationSET =
                        new SequenceExtractorTask(sequences,
                                continuationPattern,
                                continuationTypeDirectory.toFile(),
                                continuationOutputDirectory.toFile(), delimiter);
                executorService.execute(continuationSET);

            }
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    //    public void extractContinuationSequences2(int maxModelLength, int cores) {
    //        ArrayList<boolean[]> absolutePatterns =
    //                PatternBuilder.getLMPatterns(maxModelLength);
    //
    //        // initialize executerService
    //        // int cores = Runtime.getRuntime().availableProcessors();
    //        ExecutorService executorService = Executors.newFixedThreadPool(cores);
    //        for (boolean[] absolutePattern : absolutePatterns) {
    //            Path originalSequencesDirectory =
    //                    new Path(
    //                            outputDirectory.getAbsolutePath()
    //                                    + "/"
    //                                    + absoluteDirectory.getName()
    //                                    + "/"
    //                                    + PatternTransformer
    //                                            .getStringPattern(absolutePattern));
    //            Path outputDirectory =
    //                    new Path(this.outputDirectory.getAbsolutePath()
    //                            + "/continuation");
    //            ContinuationExtractorTask cet =
    //                    new ContinuationExtractorTask(originalSequencesDirectory,
    //                            absolutePattern, absoluteDirectory,
    //                            outputDirectory, wordIndex, delimiter);
    //            executorService.execute(cet);
    //        }
    //
    //        executorService.shutdown();
    //        try {
    //            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    //        } catch (InterruptedException e) {
    //            // TODO Auto-generated catch block
    //            e.printStackTrace();
    //        }
    //    }
}
