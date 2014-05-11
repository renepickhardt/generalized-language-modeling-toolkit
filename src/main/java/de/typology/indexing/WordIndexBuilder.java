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

/**
 * A class for building a text file containing a index representation for a
 * given text file based on the alphabetical distribution of its words.
 */
public class WordIndexBuilder {

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
     * @param maxCountDivider
     *            The number of <em>indexed files</em> the index should be split
     *            across.
     * @param beforeLine
     *            Prepended before each line before splitting into words. Need
     *            here because it might skew the index.
     * @param afterLine
     *            Appended after each line before splitting into words. Need
     *            here because it might skew the index.
     */
    public void buildIndex(
            InputStream input,
            OutputStream output,
            int maxCountDivider,
            String beforeLine,
            String afterLine) throws IOException {
        // calculate counts of words
        TreeMap<String, Integer> wordCounts = new TreeMap<String, Integer>();
        // TODO: buffer size calculation
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input),
                        100 * 1024 * 1024)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = beforeLine + line + afterLine;
                String[] words = line.split("\\s+");

                for (String word : words) {
                    Integer count = wordCounts.get(word);
                    wordCounts.put(word, count == null ? 1 : count + 1);
                }
            }
        }

        // summarize all word counts
        long sumCount = 0L;
        for (int count : wordCounts.values()) {
            sumCount += count;
        }

        // calculate max count per file
        Long maxCountPerFile = sumCount / maxCountDivider;
        if (maxCountPerFile < 1L) {
            maxCountPerFile = 1L;
        }

        // build index
        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(output))) {
            int fileCount = 0;
            long currentFileCount = 0L;

            for (Map.Entry<String, Integer> wordCount : wordCounts.entrySet()) {
                String word = wordCount.getKey();
                int count = wordCount.getValue();

                if (fileCount == 0
                        || currentFileCount + count > maxCountPerFile) {
                    writer.write(word + "\t" + fileCount + "\n");
                    currentFileCount = count;
                    ++fileCount;
                } else {
                    currentFileCount += count;
                }
            }
        }
    }
}
