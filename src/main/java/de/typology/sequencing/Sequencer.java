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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

/**
 * takes input text of the format 1 sentence per line 
 * and each word currently must contain a part of speech tag 
 * 
 * example: 
 * 
 * word_1/pos_1 word_2/pos_2 word_3/pos_3 ... word_n/pos_n
 * 
 * all (skipped) sequences of all lengths are extracted and stored
 * in various files (respecting the WordIndex)
 * 
 * TODO: the sequences must work with and without part of speeches
 * 
 * @author rpickhardt, lukasschmelzeisen
 *
 */
public class Sequencer {

    public static float MEMORY_FACTOR = 0.2f;

    public static long UPDATE_INTERVAL = 5 * 1000; // 5s

    private static Logger logger = LogManager.getLogger();

    private Path inputFile;

    private Path outputDir;

    private WordIndex wordIndex;

    private int maxCountDivider;

    public Sequencer(
            Path inputFile,
            Path outputDir,
            WordIndex wordIndex,
            int maxCountDivider) throws IOException {
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.wordIndex = wordIndex;
        this.maxCountDivider = maxCountDivider;
    }

    public void sequence(Set<Pattern> inputPatterns) throws IOException {
        logger.info("Sequencing training data.");

        Files.createDirectory(outputDir);

        // group patterns by length
        int maxPatternLength = 0;
        Map<Integer, Set<Pattern>> patternsByLength =
                new TreeMap<Integer, Set<Pattern>>();
        for (Pattern pattern : inputPatterns) {
            Set<Pattern> patterns = patternsByLength.get(pattern.length());
            if (patterns == null) {
                patterns = new HashSet<Pattern>();
                patternsByLength.put(pattern.length(), patterns);
                if (maxPatternLength < pattern.length()) {
                    maxPatternLength = pattern.length();
                }
            }
            patterns.add(pattern);
        }

        // sequence by length-grouped patterns
        for (Map.Entry<Integer, Set<Pattern>> entry : patternsByLength
                .entrySet()) {
            sequence(entry.getKey(), entry.getValue(), maxPatternLength);
        }
    }

    private void sequence(
            int patternLength,
            Set<Pattern> patterns,
            int maxPatternLength) throws IOException {
        logger.info("Building sequences with length: " + patternLength);

        int bufferSizes =
                (int) (MEMORY_FACTOR * (Runtime.getRuntime().maxMemory()
                        / patterns.size() / maxCountDivider));

        // open writers for patterns
        Map<Pattern, List<BufferedWriter>> patternWriters =
                new HashMap<Pattern, List<BufferedWriter>>();
        for (Pattern pattern : patterns) {
            Path dir = outputDir.resolve(pattern.toString());
            Files.createDirectory(dir);
            List<BufferedWriter> writers =
                    wordIndex.openWriters(dir, bufferSizes);
            patternWriters.put(pattern, writers);
        }

        long readSize = 0;
        long totalSize = Files.size(inputFile);
        long time = System.currentTimeMillis();

        try (InputStream inputFileSteam = Files.newInputStream(inputFile);
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(inputFileSteam),
                                100 * 1024 * 1024)) {
            String line;
            while ((line = reader.readLine()) != null) {
                readSize += line.getBytes().length;

                line = surroundWithTokens(maxPatternLength, line);

                String[] split = StringUtils.splitAtSpace(line);

                String[] words = new String[split.length];
                String[] pos = new String[split.length];
                generateWordsAndPos(split, words, pos);

                writeSequences(patternLength, patternWriters, words, pos);

                // if more then a minute since last update
                if (System.currentTimeMillis() - time >= UPDATE_INTERVAL) {
                    time = System.currentTimeMillis();
                    logger.info(String.format("%6.2f", 100.f * readSize
                            / totalSize)
                            + "%");
                }
            }
        }

        // close writers for patterns
        for (List<BufferedWriter> writers : patternWriters.values()) {
            wordIndex.closeWriters(writers);
        }
    }

    private String surroundWithTokens(int maxPatternLength, String line) {
        StringBuilder lineBuilder = new StringBuilder();
        for (int i = 1; i != maxPatternLength; ++i) {
            lineBuilder.append("<s");
            lineBuilder.append(i);
            lineBuilder.append(">/<BOS>");
        }
        lineBuilder.append(line);
        for (int i = maxPatternLength - 1; i != 0; --i) {
            lineBuilder.append("</s");
            lineBuilder.append(i);
            lineBuilder.append(">/<EOS>");
        }
        return lineBuilder.toString();
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
            String[] words,
            String[] pos) throws IOException {
        for (int p = 0; p <= words.length - patternLength; ++p) {
            for (Map.Entry<Pattern, List<BufferedWriter>> entry : patternWriters
                    .entrySet()) {
                Pattern pattern = entry.getKey();
                List<BufferedWriter> writers = entry.getValue();

                String patternSequence = pattern.apply(words, pos, p);

                // get first word of sequence that isn't PatternElem.SKIPPED_WORD
                String indexWord = PatternElem.SKIPPED_WORD;
                for (int i = 0; indexWord.equals(PatternElem.SKIPPED_WORD)
                        && i != patternLength; ++i) {
                    indexWord = pattern.get(i).apply(words[p + i], pos[p + i]);
                }

                writers.get(wordIndex.rank(indexWord)).write(
                        patternSequence + "\n");
            }
        }
    }

}
