package de.typology.extracting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.typology.patterns.Pattern;

/**
 * This class extracts all sequences that are needed for computing the
 * Kneser-Ney smoothed values for a set of given test sequences.
 */
public class TestSequenceExtractor {

    private Path absoluteDir;

    private Path continuationDir;

    private Path outputDir;

    private String delimiter;

    private int modelLength;

    private int numberOfCores;

    private Set<String> sequences;

    public TestSequenceExtractor(
            InputStream input,
            Path absoluteDir,
            Path continuationDir,
            Path outputDir,
            String delimiter,
            int modelLength,
            int numberOfCores) throws IOException {
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        this.outputDir = outputDir;
        this.delimiter = delimiter;
        this.modelLength = modelLength;
        this.numberOfCores = numberOfCores;

        Files.createDirectory(outputDir);

        sequences = new HashSet<String>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sequences.add(line);
            }
        }
    }

    public void extractAbsoluteSequences() throws IOException,
            InterruptedException {
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        List<Pattern> patterns =
                Pattern.getGlmForSmoothingPatterns(modelLength);

        Path outputBaseDir =
                outputDir.resolve(absoluteDir.getFileName());
        Files.createDirectory(outputBaseDir);

        for (Pattern pattern : patterns) {
            Path inputDir = absoluteDir.resolve(pattern.toString());
            Path outputDir =
                    outputBaseDir.resolve(pattern.toString());

            SequenceExtractorTask sequenceExtractorTask =
                    new SequenceExtractorTask(inputDir, outputDir,
                            sequences, pattern, delimiter);
            executorService.execute(sequenceExtractorTask);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    public void extractContinuationSequences() throws IOException,
            InterruptedException {
        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        Path outputBaseDir =
                outputDir.resolve(continuationDir.getFileName());
        Files.createDirectory(outputBaseDir);

        try (DirectoryStream<Path> continuationFiles =
                Files.newDirectoryStream(continuationDir)) {
            for (Path inputDir : continuationFiles) {
                String patternLabel = inputDir.getFileName().toString();
                if (patternLabel.endsWith("-split")) {
                    continue;
                }

                Pattern pattern = new Pattern(patternLabel);
                Path outputDir =
                        outputBaseDir.resolve(patternLabel);

                SequenceExtractorTask sequenceExtractorTask =
                        new SequenceExtractorTask(inputDir,
                                outputDir, sequences, pattern, delimiter);
                executorService.execute(sequenceExtractorTask);
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}
