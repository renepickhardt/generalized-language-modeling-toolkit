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

/**
 * This class takes an List of sequences and a directory of Files as an
 * input and writes all occurrences of the sequences into new files in the
 * outputDirectory
 */
public class SequenceExtractorTask implements Runnable {

    private Set<String> testingSequences;

    private boolean[] pattern;

    private Path inputDirectory;

    private Path outputDirectory;

    private String delimiter;

    public SequenceExtractorTask(
            Set<String> testingSequences,
            boolean[] pattern,
            Path inputDirectory,
            Path outputDirectory,
            String delimiter) throws IOException {
        this.testingSequences = testingSequences;
        this.pattern = pattern;
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.delimiter = delimiter;

        Files.createDirectory(outputDirectory);
    }

    @Override
    public void run() {
        try {
            Set<String> sequences =
                    generateSequences(testingSequences, pattern);

            try (DirectoryStream<Path> inputFiles =
                    Files.newDirectoryStream(inputDirectory)) {
                for (Path inputFile : inputFiles) {
                    Path outputFile =
                            outputDirectory.resolve(inputFile.getFileName());

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

    private static Set<String> generateSequences(
            Set<String> origSequences,
            boolean[] pattern) {
        Set<String> sequences = new HashSet<String>();

        for (String origSequence : origSequences) {
            // modify sequences for continuation
            // for each false at the start of pattern prepend "<dummy> "
            for (int i = 0; i != pattern.length; ++i) {
                if (pattern[i]) {
                    break;
                } else {
                    origSequence = "<dummy> " + origSequence;
                }
            }
            // for each false at the end of pattern append " <dummy>"
            for (int i = pattern.length - 1; i != -1; --i) {
                if (pattern[i]) {
                    break;
                } else {
                    origSequence += " <dummy>";
                }
            }

            String[] words = origSequence.split("\\s");
            for (int pointer = 0; pointer <= words.length - pattern.length; ++pointer) {
                // TODO: refactor sequencing from Sequencer, SequenceModifier, SequenceExtraktorTask
                String sequence = "";
                for (int i = 0; i != pattern.length; ++i) {
                    if (pattern[i]) {
                        sequence += words[pointer + i] + " ";
                    }
                }
                sequence = sequence.replaceFirst(" $", "");

                if (sequence.length() > 0) {
                    sequences.add(sequence);
                }
            }
        }

        return sequences;
    }
}
