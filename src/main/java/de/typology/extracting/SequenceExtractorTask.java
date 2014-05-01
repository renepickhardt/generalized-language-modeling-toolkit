package de.typology.extracting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class takes an List of sequences and a directory of Files as an
 * input and writes all occurrences of the sequences into new files in the
 * outputDirectory
 */
public class SequenceExtractorTask implements Runnable {

    private List<String> originalSequences;

    private boolean[] pattern;

    private Path workingDirectory;

    private Path outputDirectory;

    private String delimiter;

    public SequenceExtractorTask(
            List<String> originalSequences,
            boolean[] pattern,
            Path workingDirectory,
            Path outputDirectory,
            String delimiter) throws IOException {
        this.originalSequences = originalSequences;
        this.pattern = pattern;
        this.workingDirectory = workingDirectory;
        this.outputDirectory = outputDirectory;
        this.delimiter = delimiter;

        Files.createDirectory(outputDirectory);
    }

    @Override
    public void run() {
        try {
            Set<String> newSequences = getNewSequences();

            try (DirectoryStream<Path> trainingFiles =
                    Files.newDirectoryStream(workingDirectory)) {
                for (Path trainingFile : trainingFiles) {
                    Path outputFile =
                            outputDirectory.resolve(trainingFile.getFileName());
                    if (trainingFile.getFileName().toString().equals("all")) {
                        Files.copy(trainingFile, outputFile);
                    } else {
                        try (BufferedReader reader =
                                Files.newBufferedReader(trainingFile,
                                        Charset.defaultCharset());
                                BufferedWriter writer =
                                        Files.newBufferedWriter(outputFile,
                                                Charset.defaultCharset())) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (newSequences
                                        .contains(line.split(delimiter)[0])) {
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

    private Set<String> getNewSequences() {
        Set<String> newSequences = new HashSet<String>();

        for (String originalLine : originalSequences) {
            // modify sequences for continuation
            if (!pattern[0] || !pattern[pattern.length - 1]) {
                for (boolean element : pattern) {
                    if (element) {
                        break;
                    } else {
                        originalLine = "<dummy> " + originalLine;
                    }
                }
                for (int i = pattern.length - 1; i >= 0; i--) {
                    if (pattern[i]) {
                        break;
                    } else {
                        originalLine = originalLine + " <dummy>";
                    }
                }
            }
            String[] originalLineSplit = originalLine.split("\\s");
            int linePointer = 0;
            while (originalLineSplit.length - linePointer >= pattern.length) {

                // build current Sequence
                String currentSequence = "";
                for (int i = 0; i < pattern.length; i++) {
                    currentSequence += originalLineSplit[linePointer + i] + " ";
                }
                currentSequence = currentSequence.replaceFirst(" $", "");

                String[] currentSequenceSplit = currentSequence.split("\\s");
                String newSequence = "";
                for (int i = 0; i < pattern.length; i++) {
                    if (pattern[i]) {
                        newSequence += currentSequenceSplit[i] + " ";
                    }
                }
                newSequence = newSequence.replaceFirst(" $", "");
                if (newSequence.length() > 0) {
                    newSequences.add(newSequence);
                }

                linePointer++;
            }
        }

        return newSequences;
    }
}
