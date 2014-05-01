package de.typology.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A class for modifying the sequences in workingDirectory based on the given
 * Pattern. The modified sequences are returned as an {@link OutputStream}.
 */
public class SequenceModifierTask implements Runnable {

    private Path inputDirectory;

    private OutputStream output;

    private String delimiter;

    private boolean[] pattern;

    private boolean setCountToOne;

    public SequenceModifierTask(
            Path inputDirectory,
            OutputStream output,
            String delimiter,
            boolean[] pattern,
            boolean setCountToOne) {
        this.inputDirectory = inputDirectory;
        this.output = output;
        this.delimiter = delimiter;
        this.pattern = pattern;
        this.setCountToOne = setCountToOne;
    }

    @Override
    public void run() {
        try {
            try (BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(output));
                    DirectoryStream<Path> inputFiles =
                            Files.newDirectoryStream(inputDirectory)) {
                for (Path inputFile : inputFiles) {
                    try (BufferedReader reader =
                            Files.newBufferedReader(inputFile,
                                    Charset.defaultCharset())) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] split = line.split(delimiter);
                            String origSequence = split[0];
                            long count = Long.parseLong(split[1]);

                            // TODO: refactor sequencing from Sequencer, SequenceModifier, SequenceExtraktorTask
                            String sequence = "";
                            int i = 0;
                            for (String word : origSequence.split("\\s")) {
                                if (pattern[i]) {
                                    sequence += word + " ";
                                }
                                ++i;
                            }
                            sequence = sequence.replaceFirst(" $", "");

                            Long modifiedCount =
                                    modifyCount(origSequence, sequence, count);
                            if (modifiedCount != null) {
                                writer.write(sequence + delimiter
                                        + modifiedCount + "\n");
                            }
                        }
                    }
                }
            }

            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Long modifyCount(String origSequence, String sequence, long count)
            throws IOException {
        // for kneser-ney smoothing: every sequence that starts with <fs>
        // counts as a new sequence
        if (origSequence.matches("^(<fs>|<fs>\\s.*)$")) {
            if ((pattern.length == 1 && !pattern[0]) || pattern[0]) {
                return null;
            }

            // set <s> in _1 to zero
            // if (pattern == { false, true} && words[1].equals("<s>"))
            if ((pattern.length == 2 && !pattern[0] && pattern[1])
                    && sequence.equals("<s>")) {
                return 0L;
            }
        } else if (setCountToOne) {
            return 1L;
        }

        return count;
    }
}
