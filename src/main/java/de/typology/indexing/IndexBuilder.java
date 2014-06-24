package de.typology.indexing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.utils.StringUtils;

/**
 * A class for building a text file containing a index representation for a
 * given text file based on the alphabetical distribution of its words.
 */
public class IndexBuilder {

    private static Logger logger = LoggerFactory.getLogger(IndexBuilder.class);

    private boolean withPos;

    private boolean surroundWithTokens;

    private int maxPatternLength;

    public IndexBuilder(
            boolean withPos,
            boolean surroundWithTokens,
            int maxPatternLength) {
        this.withPos = withPos;
        this.surroundWithTokens = surroundWithTokens;
        this.maxPatternLength = maxPatternLength;
    }

    /**
     * Expects an {@code input} where line contains a number of words separated
     * by white space. Generates an output containing an index where each line
     * is formatted as: {@code <Word>\t<File>}. Where word is a {@code <Word>}
     * is a String and {@code <File>} is an Integer specifying to which
     * <em>indexed file</em> all words lying between this line's {@code <Word>}
     * and next line's belong.
     * 
     * @param input
     *            {@link InputStream} to be read.
     * @param output
     *            {@link OutputStream} to be written to.
     * @param maxWordCountDivider
     *            The number of <em>indexed files</em> the index should be split
     *            across.
     */
    public void buildIndex(
            InputStream input,
            OutputStream output,
            int maxWordCountDivider,
            int maxPosCountDivider) throws IOException {
        logger.info("Building word index.");

        // calculate counts of words and pos
        TreeMap<String, Integer> wordCounts = new TreeMap<String, Integer>();
        TreeMap<String, Integer> posCounts = new TreeMap<String, Integer>();

        // TODO: buffer size calculation
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input),
                        100 * 1024 * 1024)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (surroundWithTokens) {
                    line =
                            StringUtils.surroundWithTokens(maxPatternLength,
                                    line);
                }

                Object[] split = StringUtils.splitAtSpace(line).toArray();

                String[] words = new String[split.length];
                String[] poses = new String[split.length];
                StringUtils.generateWordsAndPos(split, words, poses, withPos);

                for (String word : words) {
                    Integer count = wordCounts.get(word);
                    wordCounts.put(word, count == null ? 1 : count + 1);
                }

                if (withPos) {
                    for (String pos : poses) {
                        Integer count = posCounts.get(pos);
                        posCounts.put(pos, count == null ? 1 : count + 1);
                    }
                }
            }
        }

        // summarize all word counts
        long sumWordCount = 0L;
        for (int count : wordCounts.values()) {
            sumWordCount += count;
        }

        // summarize all pos counts
        long sumPosCount = 0L;
        if (withPos) {
            for (int count : posCounts.values()) {
                sumPosCount += count;
            }
        }

        // calculate max count per word file
        long maxCountPerWordFile = sumWordCount / maxWordCountDivider;
        if (maxCountPerWordFile < 1L) {
            maxCountPerWordFile = 1L;
        }

        // calculate max count per pos file
        long maxCountPerPosFile = 0L;
        if (withPos) {
            maxCountPerPosFile = sumPosCount / maxPosCountDivider;
            if (maxCountPerPosFile < 1L) {
                maxCountPerPosFile = 1L;
            }
        }

        // build index
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(output))) {
            writer.write("Words:\n");

            int wordFileCount = 0;
            long currentWordFileCount = 0L;

            for (Map.Entry<String, Integer> wordCount : wordCounts.entrySet()) {
                String word = wordCount.getKey();
                int count = wordCount.getValue();

                if (wordFileCount == 0
                        || currentWordFileCount + count > maxCountPerWordFile) {
                    writer.write(word);
                    writer.write("\t");
                    writer.write(Integer.toString(wordFileCount));
                    writer.write("\n");
                    currentWordFileCount = count;
                    ++wordFileCount;
                } else {
                    currentWordFileCount += count;
                }
            }

            if (withPos) {
                writer.write("Poses:\n");

                int posFileCount = 0;
                long currentPosFileCount = 0L;

                for (Map.Entry<String, Integer> posCount : posCounts.entrySet()) {
                    String pos = posCount.getKey();
                    int count = posCount.getValue();

                    if (posFileCount == 0
                            || currentPosFileCount + count > maxCountPerPosFile) {
                        writer.write(pos);
                        writer.write("\t");
                        writer.write(Integer.toString(posFileCount));
                        writer.write("\n");
                        currentPosFileCount = count;
                        ++posFileCount;
                    } else {
                        currentPosFileCount += count;
                    }
                }
            }
        }
    }
}
