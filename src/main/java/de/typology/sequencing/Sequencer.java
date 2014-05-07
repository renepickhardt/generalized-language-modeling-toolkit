package de.typology.sequencing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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
    }

    public void sequence(Set<Pattern> inputPatterns) throws IOException {
        logger.info("Sequencing training data.");

        Files.createDirectory(outputDir);

        NavigableMap<Integer, Set<Pattern>> patternsByLength =
                new TreeMap<Integer, Set<Pattern>>();
        for (Pattern pattern : inputPatterns) {
            Set<Pattern> patterns = patternsByLength.get(pattern.length());
            if (patterns == null) {
                patterns = new HashSet<Pattern>();
                patternsByLength.put(pattern.length(), patterns);
            }
            patterns.add(pattern);
        }

        
        
        for (Map.Entry<Integer, Set<Pattern>> entry : patternsByLength.descendingMap()
                .entrySet()) {
            sequence(entry.getKey(), entry.getValue());
        }
    }

    private void sequence(int patternLength, Set<Pattern> patterns)
            throws IOException {
        logger.info("Building sequences with length: " + patternLength);

        Map<Pattern, List<BufferedWriter>> patternWriters =
                new HashMap<Pattern, List<BufferedWriter>>();
        for (Pattern pattern : patterns) {
            Path dir = outputDir.resolve(pattern.toString());
            Files.createDirectory(dir);
            List<BufferedWriter> writers = wordIndex.openWriters(dir);
            patternWriters.put(pattern, writers);
        }

        try (InputStream inputFileSteam = Files.newInputStream(inputFile);
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(inputFileSteam),
                                100 * 1024 * 1024)) {
            String line;
            while ((line = reader.readLine()) != null) {
            	//TODO: add n-1 beforeLine Tags
                line = beforeLine + line + afterLine;

                String[] split = splitAtSpace(line);

                String[] words = new String[split.length];
                String[] pos = new String[split.length];
                generateWordsAndPos(split, words, pos);

                writeSequences(patternLength, patternWriters, split, words, pos);
            }
        }

        for (List<BufferedWriter> writers : patternWriters.values()) {
            wordIndex.closeWriters(writers);
        }
    }

    private static String[] splitAtSpace(String s) {
        List<String> result = new ArrayList<String>();

        int sp1 = 0, sp2;
        while (true) {
            sp2 = s.indexOf(' ', sp1);

            if (sp2 == -1) {
                String substr = s.substring(sp1);
                if (!substr.isEmpty()) {
                    result.add(substr);
                }
                break;
            } else {
                String substr = s.substring(sp1, sp2);
                if (!substr.isEmpty()) {
                    result.add(substr);
                }
                sp1 = sp2 + 1;
            }
        }

        return result.toArray(new String[result.size()]);
    }

    private void generateWordsAndPos(
            String[] split,
            String[] words,
            String[] pos) {
        for (int i = 0; i != split.length; ++i) {
            int lastSlash = split[i].lastIndexOf('/');
            if (lastSlash == -1) {
                words[i] = split[i];
                pos[i] = "UNKP"; // unkown POS, not part of any pos-tagset 
            } else {
                words[i] = split[i].substring(0, lastSlash);
                pos[i] = split[i].substring(lastSlash + 1);
            }
        }
    }

    private void writeSequences(
            int patternLength,
            Map<Pattern, List<BufferedWriter>> patternWriters,
            String[] split,
            String[] words,
            String[] pos) throws IOException {
        for (int p = 0; p <= split.length - patternLength; ++p) {
            for (Map.Entry<Pattern, List<BufferedWriter>> entry : patternWriters
                    .entrySet()) {
                Pattern pattern = entry.getKey();
                List<BufferedWriter> writers = entry.getValue();

                String patternSequence = pattern.apply(words, pos, p);
                String firstWord = pattern.get(0).apply(words[p], pos[p]);

                writers.get(wordIndex.rank(firstWord)).write(
                        patternSequence + "\n");
            }
        }
    }

}
