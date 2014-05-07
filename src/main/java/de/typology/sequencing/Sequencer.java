package de.typology.sequencing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;

public class Sequencer {

    private static Logger logger = LogManager.getLogger();

    private Path inputFile;

    private Path outputDir;

    private WordIndex wordIndex;

    private String beforeLine;

    private String afterLine;

    public Sequencer(
            Path inputFile,
            Path outputDir,
            WordIndex wordIndex,
            String beforeLine,
            String afterLine) throws IOException {
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.wordIndex = wordIndex;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;

        Files.createDirectory(outputDir);
    }

    public void splitIntoFiles(Set<Pattern> inputPatterns) throws IOException {
        logger.info("Building sequences.");
        Map<Integer, Set<Pattern>> patternsByLength =
                new TreeMap<Integer, Set<Pattern>>();
        for (Pattern pattern : inputPatterns) {
            Set<Pattern> patterns = patternsByLength.get(pattern.length());
            if (patterns == null) {
                patterns = new HashSet<Pattern>();
                patternsByLength.put(pattern.length(), patterns);
            }
            patterns.add(pattern);
        }

        for (Map.Entry<Integer, Set<Pattern>> entry : patternsByLength
                .entrySet()) {
            splitIntoFiles(entry.getKey(), entry.getValue());
        }
    }

    private void splitIntoFiles(int patternLength, Set<Pattern> patterns)
            throws IOException {
        logger.info("Building sequences with length: " + patternLength);

        Map<Pattern, Map<Integer, BufferedWriter>> patternWriters =
                new HashMap<Pattern, Map<Integer, BufferedWriter>>();
        for (Pattern pattern : patterns) {
            Path dir = outputDir.resolve(pattern.toString());
            Files.createDirectory(dir);
            Map<Integer, BufferedWriter> writers = wordIndex.openWriters(dir);
            patternWriters.put(pattern, writers);
        }

        try (InputStream inputFileSteam = Files.newInputStream(inputFile);
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(inputFileSteam),
                                100 * 8 * 1024)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = beforeLine + line + afterLine;
                String[] words = line.split("\\s");

                for (int pointer = 0; pointer <= words.length - patternLength; ++pointer) {
                    String sequence = "";
                    boolean first = true;
                    for (int i = 0; i != patternLength; ++i) {
                        sequence += (first ? "" : " ") + words[pointer + i];
                        first = false;
                    }

                    for (Map.Entry<Pattern, Map<Integer, BufferedWriter>> entry : patternWriters
                            .entrySet()) {
                        Pattern pattern = entry.getKey();
                        Map<Integer, BufferedWriter> writers = entry.getValue();

                        String patternSequence = pattern.apply(sequence);
                        int firstSpacePos = patternSequence.indexOf(" ");
                        String firstWord =
                                (firstSpacePos == -1
                                        ? patternSequence
                                        : patternSequence.substring(0,
                                                firstSpacePos));

                        writers.get(wordIndex.rank(firstWord)).write(
                                patternSequence + "\n");
                    }
                }
            }
        }

        for (Map<Integer, BufferedWriter> writers : patternWriters.values()) {
            wordIndex.closeWriters(writers);
        }
    }
}
