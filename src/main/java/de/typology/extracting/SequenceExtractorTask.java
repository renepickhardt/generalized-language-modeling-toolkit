package de.typology.extracting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;

/**
 * Filters all files in an {@code inputDir} to only the lines that contain
 * a {@code sequence} from a set of {@code sequences}.
 */
public class SequenceExtractorTask implements Runnable {

    private Path inputDir;

    private Path outputDir;

    private Set<String> sequences;

    private String delimiter;

    /**
     * Expects an {@code inputDir} with files containing lines in
     * the format of {@code <Sequence>(<Delimiter><Count>)*}. For each file in
     * {@code inputDir} a file in {@code outputDir} will be created,
     * containing all lines whose {@code <Sequence>} was an element in
     * {@code sequences} modified by {@code pattern}.
     * 
     * @param inputDir
     *            Dir to read files from.
     * @param outputDir
     *            Dir to write files to.
     * @param sequences
     *            Set of Sequences to be checked against.
     * @param pattern
     *            Pattern {@code sequences} will be modified with.
     * @param delimiter
     *            Delimiter separating {@code <Sequence>} and {@code <Count>}s.
     */
    public SequenceExtractorTask(
            Path inputDir,
            Path outputDir,
            Set<String> sequences,
            Pattern pattern,
            String delimiter) throws IOException {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.sequences = extractSequencesWithPattern(sequences, pattern);
        this.delimiter = delimiter;

        Files.createDirectory(outputDir);
    }

    @Override
    public void run() {
        try {
            try (DirectoryStream<Path> inputFiles =
                    Files.newDirectoryStream(inputDir)) {
                for (Path inputFile : inputFiles) {
                    Path outputFile =
                            outputDir.resolve(inputFile.getFileName());

                    if (inputFile.getFileName().toString().equals("all")) {
                        Files.copy(inputFile, outputFile);
                    } else {
                        try (BufferedReader reader =
                                Files.newBufferedReader(inputFile,
                                        Charset.defaultCharset());
                                BufferedWriter writer =
                                        Files.newBufferedWriter(outputFile,
                                                Charset.defaultCharset())) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String inputSequence = line.split(delimiter)[0];
                                if (sequences.contains(inputSequence)) {
                                    writer.write(line + "\n");
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> extractSequencesWithPattern(
            Set<String> origSequences,
            Pattern pattern) {
        Set<String> sequences = new HashSet<String>();

        for (String origSequence : origSequences) {
            // modify sequences for continuation
            // for each false at the start of pattern prepend "<dummy> "
            for (int i = 0; i != pattern.length(); ++i) {
                if (pattern.get(i) == PatternElem.CNT) {
                    break;
                } else {
                    origSequence = "<dummy> " + origSequence;
                }
            }
            // for each false at the end of pattern append " <dummy>"
            for (int i = pattern.length() - 1; i != -1; --i) {
                if (pattern.get(i) == PatternElem.CNT) {
                    break;
                } else {
                    origSequence += " <dummy>";
                }
            }

            String[] words = origSequence.split("\\s");
            for (int pointer = 0; pointer <= words.length - pattern.length(); ++pointer) {
                // TODO: refactor sequencing from Sequencer, SequenceModifier, SequenceExtraktorTask
                String sequence = "";
                int i = 0;
                for (PatternElem elem : pattern) {
                    if (elem == PatternElem.CNT) {
                        sequence += words[pointer + i] + " ";
                    }
                    ++i;
                }
                sequence = sequence.replaceFirst(" $", "");
                sequences.add(sequence);
            }
        }

        return sequences;
    }
}
