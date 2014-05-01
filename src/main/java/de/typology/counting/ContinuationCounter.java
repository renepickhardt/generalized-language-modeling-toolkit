package de.typology.counting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexing.WordIndex;
import de.typology.patterns.PatternBuilder;
import de.typology.patterns.PatternTransformer;

/**
 * Counts continuation counts of sequences for a number of patterns using
 * absolute counts.
 */
public class ContinuationCounter {

    private Path inputDirectory;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private String delimiter;

    private int numberOfCores;

    private boolean deleteTempFiles;

    private Logger logger = LogManager.getLogger(getClass().getName());

    public ContinuationCounter(
            Path inputDirectory,
            Path outputDirectory,
            WordIndex wordIndex,
            String delimiter,
            int numberOfCores,
            boolean deleteTempFiles) throws IOException {
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.delimiter = delimiter;
        this.numberOfCores = numberOfCores;
        this.deleteTempFiles = deleteTempFiles;

        Files.createDirectory(outputDirectory);
    }

    public void split(List<boolean[]> patterns) throws IOException,
            InterruptedException {
        Map<boolean[], boolean[]> continuationMap =
                generateContinuationMap(patterns);

        Set<boolean[]> finishedPatterns = new HashSet<boolean[]>();

        while (finishedPatterns.size() != continuationMap.size()) {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            Set<boolean[]> currentPatterns = new HashSet<boolean[]>();

            for (Entry<boolean[], boolean[]> entry : continuationMap.entrySet()) {
                boolean[] key = entry.getKey();
                boolean[] value = entry.getValue();

                if (finishedPatterns.contains(key)) {
                    continue;
                }

                if (!PatternTransformer.getStringPattern(value).contains("0")) {
                    currentPatterns.add(key);
                    calcFromAbsolute(executorService, key, value);
                } else if (finishedPatterns.contains(value)) {
                    currentPatterns.add(key);
                    calcFromContinuation(executorService, key, value);
                }
            }

            if (currentPatterns.isEmpty()) {
                throw new IllegalStateException(
                        "No new pattern calculate for this round of calculation, algorithm won't finish.");
            }

            finishedPatterns.addAll(currentPatterns);

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            logger.info("end of this round of calculation");
        }

    }

    private void calcFromAbsolute(
            ExecutorService executorService,
            boolean[] key,
            boolean[] value) throws IOException {
        logger.info("calculate continuation counts for "
                + PatternTransformer.getStringPattern(key) + "\tfrom absolute "
                + PatternTransformer.getStringPattern(value));

        Path inputDir =
                inputDirectory.resolve(PatternTransformer
                        .getStringPattern(value));

        boolean[] outputPattern =
                PatternTransformer.getBooleanPattern(PatternTransformer
                        .getStringPattern(key).replaceAll("0", ""));
        String outputPatternLabel =
                PatternTransformer.getStringPattern(key).replaceAll("0", "_");

        splitType(executorService, inputDir, outputDirectory, outputPattern,
                outputPatternLabel, key, wordIndex, true);
    }

    private void calcFromContinuation(
            ExecutorService executorService,
            boolean[] key,
            boolean[] value) throws IOException {
        logger.info("calculate continuation counts for "
                + PatternTransformer.getStringPattern(key)
                + "\tfrom continuation "
                + PatternTransformer.getStringPattern(value));

        Path inputDir =
                outputDirectory.resolve(PatternTransformer.getStringPattern(
                        value).replaceAll("0", "_"));

        boolean[] outputPattern =
                PatternTransformer.getBooleanPattern(PatternTransformer
                        .getStringPattern(key).replaceAll("0", ""));
        String outputPatternLabel =
                PatternTransformer.getStringPattern(key).replaceAll("0", "_");

        boolean[] patternForModifier =
                new boolean[Integer.bitCount(PatternTransformer
                        .getIntPattern(value))];
        int patternPointer = 0;
        for (int i = 0; i < value.length; i++) {
            if (key[i] && value[i]) {
                patternForModifier[patternPointer] = true;
                ++patternPointer;
            } else if (!key[i] && value[i]) {
                patternForModifier[patternPointer] = false;
                ++patternPointer;
            }
        }
        splitType(executorService, inputDir, outputDirectory, outputPattern,
                outputPatternLabel, patternForModifier, wordIndex, false);
    }

    private void splitType(
            ExecutorService executorService,
            Path inputDir,
            Path outputDir,
            boolean[] pattern,
            String patternLabel,
            boolean[] patternForModifier,
            WordIndex wordIndex,
            boolean setCountToOne) throws IOException {
        PipedInputStream input = new PipedInputStream(100 * 8 * 1024);
        OutputStream output = new PipedOutputStream(input);

        // PRODUCER ////////////////////////////////////////////////////////////

        SequenceModifier sequenceModifier =
                new SequenceModifier(inputDir, output, delimiter,
                        patternForModifier, setCountToOne);
        executorService.execute(sequenceModifier);

        // CONSUMER ////////////////////////////////////////////////////////////

        Path consumerOutputDirectory = outputDir.resolve(patternLabel);

        // if pattern has only falses
        if (PatternTransformer.getIntPattern(pattern) == 0) {
            Files.createDirectory(consumerOutputDirectory);
            Path lineCountOutputPath = consumerOutputDirectory.resolve("all");

            OutputStream lineCounterOutput =
                    Files.newOutputStream(lineCountOutputPath);

            LineCounterTask lineCountTask =
                    new LineCounterTask(input, lineCounterOutput, delimiter,
                            setCountToOne);
            executorService.execute(lineCountTask);
        } else {
            // don't add tags here
            PatternCounterTask splitterTask =
                    new PatternCounterTask(input, consumerOutputDirectory,
                            wordIndex, pattern, delimiter, "", "", true,
                            deleteTempFiles);
            executorService.execute(splitterTask);
        }

    }

    private static Map<boolean[], boolean[]> generateContinuationMap(
            List<boolean[]> patterns) {
        Map<boolean[], boolean[]> map = new HashMap<boolean[], boolean[]>();

        for (boolean[] pattern : patterns) {
            addPatterns(map, pattern, pattern, 0);
        }

        // Filter entries if:
        // - key == value
        // - !key[0] && !key[1]

        Map<boolean[], boolean[]> filteredMap =
                new HashMap<boolean[], boolean[]>();

        for (Entry<boolean[], boolean[]> entry : map.entrySet()) {
            boolean[] key = entry.getKey();
            boolean[] value = entry.getValue();

            if (Arrays.equals(key, value)) {
                continue;
            }

            if (key.length > 2 && !key[0] && !key[1]) {
                continue;
            }

            filteredMap.put(key, value);
        }

        return filteredMap;
    }

    private static void addPatterns(
            Map<boolean[], boolean[]> map,
            boolean[] pattern,
            boolean[] oldPattern,
            int position) {
        if (position < pattern.length) {
            boolean[] newPattern = pattern.clone();
            newPattern[position] = false;
            map.put(newPattern, pattern);
            map.put(pattern, oldPattern);
            addPatterns(map, newPattern, pattern, position + 1);
            addPatterns(map, pattern, oldPattern, position + 1);
        }
    }

    // DEBUG FUNCTIONS /////////////////////////////////////////////////////////

    private static void printMap(Map<boolean[], boolean[]> map) {
        System.out.println("Map: {");
        for (Map.Entry<boolean[], boolean[]> entry : map.entrySet()) {
            System.out.println("    "
                    + PatternTransformer.getStringPattern(entry.getKey())
                    + " -> "
                    + PatternTransformer.getStringPattern(entry.getValue()));
        }
        System.out.println("}");
    }

    private static void printList(List<boolean[]> list) {
        System.out.println("List: {");
        for (boolean[] pattern : list) {
            System.out.println("    "
                    + PatternTransformer.getStringPattern(pattern) + ",");
        }
        System.out.println("}");
    }

    public static void main(String[] args) {
        List<boolean[]> patterns = PatternBuilder.getReverseLMPatterns(5);
        printList(patterns);

        Map<boolean[], boolean[]> continuationMap =
                generateContinuationMap(patterns);
        printMap(continuationMap);
    }

}
