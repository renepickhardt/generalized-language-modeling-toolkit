package de.typology.counting;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;

/**
 * Counts continuation counts of sequences for a number of patterns using
 * absolute counts.
 */
public class ContinuationCounter {

    private static Logger logger = LogManager.getLogger();

    private Path inputDirectory;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private String delimiter;

    private int numberOfCores;

    private boolean deleteTempFiles;

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

    public void split(List<Pattern> patterns) throws IOException,
            InterruptedException {
        Map<Pattern, Pattern> continuationMap =
                generateContinuationMap(patterns);

        Set<Pattern> finishedPatterns = new HashSet<Pattern>();

        while (finishedPatterns.size() != continuationMap.size()) {
            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            Set<Pattern> currentPatterns = new HashSet<Pattern>();

            for (Entry<Pattern, Pattern> entry : continuationMap.entrySet()) {
                Pattern key = entry.getKey();
                Pattern value = entry.getValue();

                if (finishedPatterns.contains(key)) {
                    continue;
                }

                if (value.containsNoSkp()) {
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
            Pattern key,
            Pattern value) throws IOException {
        logger.info("calculate continuation counts for " + key
                + "\tfrom absolute " + value);

        Path inputDir = inputDirectory.resolve(value.toString());

        Pattern outputPattern = Pattern.newWithoutSkp(key);
        String outputPatternLabel = key.toString();

        splitType(executorService, inputDir, outputDirectory, outputPattern,
                outputPatternLabel, key, wordIndex, true);
    }

    private void calcFromContinuation(
            ExecutorService executorService,
            Pattern key,
            Pattern value) throws IOException {
        logger.info("calculate continuation counts for " + key
                + "\tfrom continuation " + value);

        Path inputDir = outputDirectory.resolve(value.toString());

        Pattern outputPattern = Pattern.newWithoutSkp(key);
        String outputPatternLabel = key.toString();

        List<PatternElem> patternForModifier = new ArrayList<PatternElem>();
        for (int i = 0; i != value.length(); ++i) {
            if (key.get(i) == PatternElem.CNT
                    && value.get(i) == PatternElem.CNT) {
                patternForModifier.add(PatternElem.CNT);
            } else if (key.get(i) == PatternElem.SKP
                    && value.get(i) == PatternElem.CNT) {
                patternForModifier.add(PatternElem.SKP);
            }
        }

        splitType(executorService, inputDir, outputDirectory, outputPattern,
                outputPatternLabel, new Pattern(patternForModifier), wordIndex,
                false);
    }

    private void splitType(
            ExecutorService executorService,
            Path inputDir,
            Path outputDir,
            Pattern pattern,
            String patternLabel,
            Pattern patternForModifier,
            WordIndex wordIndex,
            boolean setCountToOne) throws IOException {
        PipedInputStream input = new PipedInputStream(100 * 8 * 1024);
        OutputStream output = new PipedOutputStream(input);

        // PRODUCER ////////////////////////////////////////////////////////////

        SequenceModifierTask sequenceModifierTask =
                new SequenceModifierTask(inputDir, output, delimiter,
                        patternForModifier, setCountToOne);
        executorService.execute(sequenceModifierTask);

        // CONSUMER ////////////////////////////////////////////////////////////

        Path consumerOutputDirectory = outputDir.resolve(patternLabel);

        // if pattern has only falses
        if (pattern.length() == 0) {
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

    private static Map<Pattern, Pattern> generateContinuationMap(
            List<Pattern> patterns) {
        Map<Pattern, Pattern> map = new HashMap<Pattern, Pattern>();

        for (Pattern pattern : patterns) {
            addPatterns(map, pattern, pattern, 0);
        }

        // Filter entries if:
        // - key == value
        // - !key[0] && !key[1]

        Map<Pattern, Pattern> filteredMap = new HashMap<Pattern, Pattern>();

        for (Entry<Pattern, Pattern> entry : map.entrySet()) {
            Pattern key = entry.getKey();
            Pattern value = entry.getValue();

            if (key.equals(value)) {
                continue;
            }

            if (key.length() > 2 && key.get(0) == PatternElem.SKP
                    && key.get(1) == PatternElem.SKP) {
                continue;
            }

            filteredMap.put(key, value);
        }

        return filteredMap;
    }

    private static void addPatterns(
            Map<Pattern, Pattern> map,
            Pattern pattern,
            Pattern oldPattern,
            int position) {
        if (position < pattern.length()) {
            Pattern newPattern = pattern.clone();
            newPattern.set(position, PatternElem.SKP);
            map.put(newPattern, pattern);
            map.put(pattern, oldPattern);
            addPatterns(map, newPattern, pattern, position + 1);
            addPatterns(map, pattern, oldPattern, position + 1);
        }
    }

    // DEBUG FUNCTIONS /////////////////////////////////////////////////////////

    private static void printMap(Map<Pattern, Pattern> map) {
        System.out.println("Map: {");
        for (Map.Entry<Pattern, Pattern> entry : map.entrySet()) {
            System.out.println("    " + entry.getKey() + " -> "
                    + entry.getValue());
        }
        System.out.println("}");
    }

    private static void printList(List<Pattern> list) {
        System.out.println("List: {");
        for (Pattern pattern : list) {
            System.out.println("    " + pattern + ",");
        }
        System.out.println("}");
    }

    public static void main(String[] args) {
        List<Pattern> patterns = Pattern.getReverseLmPatterns(5);
        printList(patterns);

        Map<Pattern, Pattern> continuationMap =
                generateContinuationMap(patterns);
        printMap(continuationMap);
    }

}
