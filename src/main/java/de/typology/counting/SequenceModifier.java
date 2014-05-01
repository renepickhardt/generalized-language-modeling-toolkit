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
 * Pattern. The modified sequences are returned as outputStream
 * 
 * @author Martin Koerner
 * 
 */
public class SequenceModifier implements Runnable {

    private Path inputDirectory;

    private OutputStream output;

    private String delimiter;

    private boolean[] pattern;

    private boolean modifyCount;

    private boolean setCountToOne;

    public SequenceModifier(
            Path inputDirectory,
            OutputStream output,
            String delimiter,
            boolean[] pattern,
            boolean modifyCount,
            boolean setCountToOne) {
        this.inputDirectory = inputDirectory;
        this.output = output;
        this.delimiter = delimiter;
        this.pattern = pattern;
        this.modifyCount = modifyCount;
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
                        if (modifyCount) {
                            modifyCount(reader, writer);
                        } else {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                writer.write(line + "\n");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void modifyCount(BufferedReader reader, BufferedWriter writer)
            throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split(delimiter);
            String sequence = split[0];
            String count = split[1];

            String[] words = sequence.split("\\s");

            String patternedSequence = "";
            for (int i = 0; i < pattern.length; i++) {
                if (pattern[i]) {
                    patternedSequence += words[i] + " ";
                }
            }
            patternedSequence = patternedSequence.replaceFirst(" $", "");

            if (words[0].equals("<fs>")) {
                // for kneser-ney smoothing: every sequence that
                // starts
                // with <fs> counts as a new sequence
                if (inputDirectory.getFileName().toString().equals("1")) {
                    continue;
                }

                if (!pattern[0]) {
                    // set <s> in _1 to zero
                    if (inputDirectory.getFileName().toString().equals("11")
                            && words[1].equals("<s>")) {
                        writer.write("<s>" + delimiter + "0\n");
                    } else {
                        writer.write(patternedSequence + delimiter
                                + line.split(delimiter)[1] + "\n");
                    }
                }
                // else if (pattern[0]) { leave out sequence }
            } else {
                if (setCountToOne) {
                    writer.write(patternedSequence + delimiter + "1\n");
                } else {
                    writer.write(patternedSequence + delimiter + count + "\n");
                }
            }
        }
    }
}
