package de.typology.filtering;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.typology.indexing.WordIndex;
import de.typology.patterns.PatternBuilder;
import de.typology.patterns.PatternTransformer;

public class Filterer {

    private Path inputFile;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private String beforeLine;

    private String afterLine;

    private int modelLength;

    private int numberOfCores;

    private boolean deleteTempFiles;

    public Filterer(
            Path inputFile,
            Path outputDirectory,
            WordIndex wordIndex,
            String beforeLine,
            String afterLine,
            int modelLength,
            int numberOfCores,
            boolean deleteTempFiles) throws IOException {
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.modelLength = modelLength;
        this.numberOfCores = numberOfCores;
        this.deleteTempFiles = deleteTempFiles;

        Files.createDirectory(outputDirectory);
    }

    public void filter() throws IOException, InterruptedException {
        List<boolean[]> patterns =
                PatternBuilder.getGLMForSmoothingPatterns(modelLength);

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfCores);

        for (boolean[] pattern : patterns) {
            String patternLabel = PatternTransformer.getStringPattern(pattern);

            InputStream input = Files.newInputStream(inputFile);

            FiltererTask filtererTask =
                    new FiltererTask(input,
                            outputDirectory.resolve(patternLabel), wordIndex,
                            pattern, beforeLine, afterLine, deleteTempFiles);
            executorService.execute(filtererTask);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}
