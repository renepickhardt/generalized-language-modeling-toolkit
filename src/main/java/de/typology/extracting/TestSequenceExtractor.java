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

    private Path absoluteDirectory;

    private Path continuationDirectory;

    private Path outputDirectory;

    private String delimiter;

    private int modelLength;

    private int numberOfCores;

    private List<String> sequences;

    public TestSequenceExtractor(
            Path testSequenceFile,
            Path absoluteDirectory,
            Path continuationDirectory,
            Path outputDirectory,
            String delimiter,
            int modelLength,
            int numberOfCores) throws IOException {
        this.absoluteDirectory = absoluteDirectory;
        this.continuationDirectory = continuationDirectory;
        this.outputDirectory = outputDirectory;
        this.delimiter = delimiter;
        this.modelLength = modelLength;
        this.numberOfCores = numberOfCores;

        Files.createDirectory(outputDirectory);

        sequences = new ArrayList<String>();
        try (BufferedReader testSequenceReader =
                Files.newBufferedReader(testSequenceFile,
                        Charset.defaultCharset())) {
            String line;
            while ((line = testSequenceReader.readLine()) != null) {
                sequences.add(line);
            }
        }
    }

    public void extractAbsoluteSequences() throws IOException,
            InterruptedException {
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        List<boolean[]> absolutePatterns =
                PatternBuilder.getGLMForSmoothingPatterns(modelLength);

        for (boolean[] absolutePattern : absolutePatterns) {
            String absoluteStringPattern =
                    PatternTransformer.getStringPattern(absolutePattern);
            Path absoluteworkingDirectory =
                    absoluteDirectory.resolve(absoluteStringPattern);
            Path absoluteOutputDirectory =
                    outputDirectory.resolve(absoluteDirectory.getFileName())
                            .resolve(absoluteStringPattern);

            SequenceExtractorTask sequenceExtractorTask =
                    new SequenceExtractorTask(sequences, absolutePattern,
                            absoluteworkingDirectory.toFile(),
                            absoluteOutputDirectory.toFile(), delimiter);
            executorService.execute(sequenceExtractorTask);

        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public void extractContinuationSequences() throws IOException,
            InterruptedException {
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        try (DirectoryStream<Path> continuationFiles =
                Files.newDirectoryStream(continuationDirectory)) {
            for (Path continuationTypeDirectory : continuationFiles) {
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

                SequenceExtractorTask sequenceExtractorTask =
                        new SequenceExtractorTask(sequences,
                                continuationPattern,
                                continuationTypeDirectory.toFile(),
                                continuationOutputDirectory.toFile(), delimiter);
                executorService.execute(sequenceExtractorTask);

            }
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}
