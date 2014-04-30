package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import de.typology.indexes.WordIndex;

/**
 * A class for splitting a text file (via inputStream) into sequences that are
 * stored in different files based on the indexFile in outputDirectory.
 * 
 * @author Martin Koerner
 * 
 */
public class Sequencer {

    private InputStream inputStream;

    private File outputDirectory;

    private WordIndex wordIndex;

    private boolean[] pattern;

    private String beforeLine;

    private String afterLine;

    private String delimiter;

    private boolean completeLine;

    public Sequencer(
            InputStream inputStream,
            File outputDirectory,
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
                    writers.get(wordIndex.rank(words[0])).write(line + "\n");
                } else {
                    int linePointer = 0;
                    while (words.length - linePointer >= pattern.length) {
                        String sequence = "";
                        for (int i = 0; i < pattern.length; i++) {
                            if (pattern[i]) {
                                sequence += words[linePointer + i] + " ";
                            }
                        }
                        sequence = sequence.replaceFirst(" $", "");
                        sequence += delimiter + "1\n";

                        // write sequence

                        writers.get(wordIndex.rank(sequence.split(" ")[0]))
                                .write(sequence);

                        linePointer++;
                    }
                }
            }

            wordIndex.closeWriters(writers);
        }
    }

}
