package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;

import de.typology.indexes.WordIndex;

/**
 * Splits an {@link InputStream} into a sequences of a pattern.
 */
public class Sequencer {

    private InputStream inputStream;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private boolean[] pattern;

    private String beforeLine;

    private String afterLine;

    private String delimiter;

    private boolean completeLine;

    /**
     * Expects an {@code inputStream} where each line contains a number of words
     * separated by whitespace. Extracts all sequence specified by
     * {@code pattern} and writes them to <em>indexed files</em> in
     * {@code outputDirectory}. Each output line has this format:
     * {@code <Sequence><Delimiter>1}.
     * 
     * @param inputStream
     *            {@link InputStream} to be read.
     * @param outputDirectory
     *            Directory where <em>indexed files</em> should be
     *            written to.
     * @param wordIndex
     *            {@link WordIndex} of the corpus.
     * @param pattern
     *            Pattern about which Sequence should be extracted.
     * @param beforeLine
     *            Prepended before each line before sequencing.
     * @param afterLine
     *            Appended after each line before sequencing.
     * @param delimiter
     *            Delimiter between {@code Sequence} and {@code Count} in the
     *            output.
     * @param completeLine
     *            If {@code true} will not extract Sequences or append Count
     *            but instead just write each line into the correct
     *            <em>indexed file</em>.
     *            If {@code false} will act as described above.
     */
    public Sequencer(
            InputStream inputStream,
            Path outputDirectory,
            WordIndex wordIndex,
            boolean[] pattern,
            String beforeLine,
            String afterLine,
            String delimiter,
            boolean completeLine) {
        this.inputStream = inputStream;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.delimiter = delimiter;
        this.completeLine = completeLine;
    }

    /**
     * Perform the actual splitting and writing output.
     */
    public void splitIntoFiles() throws IOException {
        // TODO: bufferSize calculation
        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream),
                        100 * 8 * 1024)) {
            HashMap<Integer, BufferedWriter> writers =
                    wordIndex.openWriters(outputDirectory);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = beforeLine + line + afterLine;
                String[] words = line.split("\\s");

                if (completeLine) {
                    BufferedWriter writer =
                            writers.get(wordIndex.rank(words[0]));
                    writer.write(line + "\n");
                } else {
                    for (int pointer = 0; pointer <= words.length
                            - pattern.length; ++pointer) {
                        String sequence = "";
                        for (int i = 0; i != pattern.length; ++i) {
                            if (pattern[i]) {
                                sequence += words[pointer + i] + " ";
                            }
                        }
                        sequence = sequence.replaceFirst(" $", "");
                        sequence += delimiter + "1\n";

                        BufferedWriter writer =
                                writers.get(wordIndex.rank(sequence.split(" ")[0]));
                        writer.write(sequence);
                    }
                }
            }

            wordIndex.closeWriters(writers);
        }
    }

}
