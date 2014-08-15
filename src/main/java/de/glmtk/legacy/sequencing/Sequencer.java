package de.glmtk.legacy.sequencing;

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

import de.glmtk.legacy.indexing.Index;
import de.glmtk.legacy.indexing.IndexWriter;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StringUtils;

/**
 * takes input text of the format 1 sentence per line
 * and each word currently must contain a part of speech tag.
 *
 * example:
 *
 * {@code word_1/pos_1 word_2/pos_2 word_3/pos_3 ... word_n/pos_n}
 *
 * all (skipped) sequences of all lengths are extracted and stored
 * in various files (respecting the WordIndex)
 *
 * TODO: the sequences must work with and without part of speeches
 */
public class Sequencer {

    public static float MEMORY_FACTOR = 0.2f;

    public static long UPDATE_INTERVAL = 5 * 1000; // 5s

    private static Logger logger = LogManager
            .getFormatterLogger(Sequencer.class);

    private Path inputFile;

    private Path outputDir;

    private Index wordIndex;

    private int maxCountDivider;

    private boolean withPos;

    private boolean surroundWithTokens;

    public Sequencer(
            Path inputFile,
            Path outputDir,
            Index wordIndex,
            int maxCountDivider,
            boolean withPos,
            boolean surroundWithTokens) throws IOException {
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.wordIndex = wordIndex;
        this.maxCountDivider = maxCountDivider;
        this.withPos = withPos;
        this.surroundWithTokens = surroundWithTokens;
    }

    public void sequence(Set<Pattern> inputPatterns) throws IOException {
        logger.info("Sequencing training data.");

        Files.createDirectory(outputDir);

        // group patterns by length
        int maxPatternLength = 0;
        Map<Integer, Set<Pattern>> patternsByLength =
                new TreeMap<Integer, Set<Pattern>>();
        for (Pattern pattern : inputPatterns) {
            if (!withPos && pattern.containsPos()) {
                throw new IllegalStateException(
                        "Cant have POS pattern without withPos = true.");
            }
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

    @SuppressWarnings("deprecation")
    private void sequence(
            int patternLength,
            Set<Pattern> patterns,
            int maxPatternLength) throws IOException {
        logger.info("Building sequences with length: %s", patternLength);

        int bufferSizes =
                (int) (MEMORY_FACTOR * (Runtime.getRuntime().maxMemory()
                        / patterns.size() / maxCountDivider));

        // open writers for patterns
        Map<Pattern, IndexWriter> patternWriters =
                new HashMap<Pattern, IndexWriter>();
        for (Pattern pattern : patterns) {
            Path dir = outputDir.resolve(pattern.toString());
            Files.createDirectory(dir);
            patternWriters.put(pattern,
                    wordIndex.openIndexWriter(pattern, dir, bufferSizes));
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

                if (surroundWithTokens) {
                    line =
                            StringUtils.surroundWithTokens(maxPatternLength,
                                    line);
                }

                Object[] split = StringUtils.splitAtChar(line, ' ').toArray();

                String[] words = new String[split.length];
                String[] poses = new String[split.length];
                StringUtils.generateWordsAndPos(split, words, poses, withPos);

                writeSequences(patternLength, patternWriters, words, poses);

                // if more then a minute since last update
                if (System.currentTimeMillis() - time >= UPDATE_INTERVAL) {
                    time = System.currentTimeMillis();
                    logger.info("%6.2f%%", 100.f * readSize / totalSize);
                }
            }
        }

        // close writers for patterns
        for (IndexWriter indexWriter : patternWriters.values()) {
            indexWriter.close();
        }
    }

    private void writeSequences(
            int patternLength,
            Map<Pattern, IndexWriter> patternWriters,
            String[] words,
            String[] poses) throws IOException {
        for (int p = 0; p <= words.length - patternLength; ++p) {
            for (Map.Entry<Pattern, IndexWriter> entry : patternWriters
                    .entrySet()) {
                Pattern pattern = entry.getKey();
                IndexWriter indexWriter = entry.getValue();

                String patternSequence = pattern.apply(words, poses, p);

                BufferedWriter writer = indexWriter.get(words, poses, p);
                writer.write(patternSequence);
                writer.write("\n");
            }
        }
    }

}
