package de.typology.sequencing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import de.typology.indexing.WordIndex;
import de.typology.patterns.Pattern;

/**
 * Splits an {@link InputStream} into a sequences of a pattern.
 */
public class Sequencer {

    private Path inputFile;

    private Path outputDirectory;

    private WordIndex wordIndex;

    private List<Pattern> pattern;

    private String beforeLine;

    private String afterLine;

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
     * @param pattern
     *            Pattern specifying sequences.
     * @param beforeLine
     *            Prepended before each line before sequencing.
     * @param afterLine
     *            Appended after each line before sequencing.
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
            Path inputFile,
            Path outputDirectory,
            WordIndex wordIndex,
            List<Pattern> pattern,
            String beforeLine,
            String afterLine,
            boolean setCountToOne,
            String delimiter) {
        this.inputFile = inputFile;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.beforeLine = beforeLine;
        this.afterLine = afterLine;
        this.setCountToOne = setCountToOne;
        this.delimiter = delimiter;
    }

    /**
     * Perform the actual splitting and writing output.
     */
    public void splitIntoFiles() throws IOException {
        // TODO: bufferSize calculation
        //        try (BufferedReader bufferedReader =
        //                new BufferedReader(new InputStreamReader(inputFile),
        //                        100 * 8 * 1024)) {
        //            Map<Integer, BufferedWriter> writers =
        //                    wordIndex.openWriters(outputDirectory);
        //
        //            String line;
        //            while ((line = bufferedReader.readLine()) != null) {
        //                line = beforeLine + line + afterLine;
        //                String[] words = line.split("\\s");
        //
        //                if (onlyLine) {
        //                    BufferedWriter writer =
        //                            writers.get(wordIndex.rank(words[0]));
        //                    writer.write(line + "\n");
        //                } else {
        //                    for (int pointer = 0; pointer <= words.length
        //                            - pattern.length(); ++pointer) {
        //                        // TODO: refactor sequencing from Sequencer, SequenceModifier, SequenceExtraktorTask
        //                        String sequence = "";
        //                        int i = 0;
        //                        for (PatternElem elem : pattern) {
        //                            if (elem == PatternElem.CNT) {
        //                                sequence += words[pointer + i] + " ";
        //                            }
        //                            ++i;
        //                        }
        //                        sequence = sequence.replaceFirst(" $", "");
        //
        //                        BufferedWriter writer =
        //                                writers.get(wordIndex.rank(sequence.split(" ")[0]));
        //                        writer.write(sequence
        //                                + (setCountToOne ? delimiter + "1" : "") + "\n");
        //                    }
        //                }
        //            }
        //
        //            wordIndex.closeWriters(writers);
        //        }
        //
        //        inputFile.close();
    }

}
