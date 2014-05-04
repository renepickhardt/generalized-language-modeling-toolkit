package de.typology;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;

import de.typology.filtering.Filter;
import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternType;

/**
 * Splits an {@link InputStream} into a sequences of a pattern.
 */
public class Sequencer {

    private InputStream input;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private Filter filter;

    private Pattern pattern;

    private String beforeLine;

    private String afterLine;

    private boolean onlyLine;

    private boolean setCountToOne;

    private String delimiter;

    /**
     * Expects an {@code input} where each line contains a number of words
     * separated by whitespace. Extracts all sequence specified by
     * {@code pattern} and writes them to <em>indexed files</em> in
     * {@code outputDirectory}. Each output line has this format:
     * {@code <Sequence>}.
     * 
     * @param input
     *            {@link InputStream} to be read.
     * @param outputDirectory
     *            Directory where <em>indexed files</em> should be
     *            written to.
     * @param wordIndex
     *            {@link WordIndex} of the corpus.
     * @param filter
     *            Can be {@code null}. If a {@link Filter} is given will
     *            test for all Sequences whether it's present in the filter. If
     *            {@code null} all Sequences will pass.
     * @param pattern
     *            Pattern specifying sequences.
     * @param beforeLine
     *            Prepended before each line before sequencing.
     * @param afterLine
     *            Appended after each line before sequencing.
     * @param onlyLine
     *            If {@code true} will not extract Sequences or append Count
     *            but instead just write each line into the correct
     *            <em>indexed file</em>.
     *            If {@code false} will act as described above.
     * @param setCountToOne
     *            If {@code false} will act as described above.
     *            If {@code true} will also append {@code <Delimiter>1} after
     *            each {@code <Sequence>}.
     * @param delimiter
     *            Delimiter between {@code Sequence} and {@code Count} in the
     *            output. Can be {@code null} if {@code setCountToOne} is
     *            {@code false}.
     */
    public Sequencer(
            InputStream input,
            Path outputDirectory,
            WordIndex wordIndex,
            Filter filter,
            Pattern pattern,
            String beforeLine,
            String afterLine,
            boolean onlyLine,
            boolean setCountToOne,
            String delimiter) {
        this.input = input;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.filter = filter;
        this.pattern = pattern;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.onlyLine = onlyLine;
        this.setCountToOne = setCountToOne;
        this.delimiter = delimiter;
    }

    /**
     * Perform the actual splitting and writing output.
     */
    public void splitIntoFiles() throws IOException {
        // TODO: bufferSize calculation
        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(input), 100 * 8 * 1024)) {
            Map<Integer, BufferedWriter> writers =
                    wordIndex.openWriters(outputDirectory);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = beforeLine + line + afterLine;
                String[] words = line.split("\\s");

                if (onlyLine) {
                    BufferedWriter writer =
                            writers.get(wordIndex.rank(words[0]));
                    writer.write(line + "\n");
                } else {
                    for (int pointer = 0; pointer <= words.length
                            - pattern.length(); ++pointer) {
                        // TODO: refactor sequencing from Sequencer, SequenceModifier, SequenceExtraktorTask
                        String sequence = "";
                        //                        for (int i = 0; i != pattern.length(); ++i) {
                        //                            if (pattern[i]) {
                        int i = 0;
                        for (PatternType p : pattern) {
                            if (p == PatternType.CNT) {
                                sequence += words[pointer + i] + " ";
                            }
                            ++i;
                        }
                        //                            }
                        //                        }
                        sequence = sequence.replaceFirst(" $", "");

                        BufferedWriter writer =
                                writers.get(wordIndex.rank(sequence.split(" ")[0]));
                        if (filter == null
                                || filter.contains(sequence, pattern)) {
                            writer.write(sequence
                                    + (setCountToOne ? delimiter + "1" : "")
                                    + "\n");
                        }
                    }
                }
            }

            wordIndex.closeWriters(writers);
        }

        input.close();
    }

}
