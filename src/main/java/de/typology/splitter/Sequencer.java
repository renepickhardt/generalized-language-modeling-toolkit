package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.indexes.WordIndex;

/**
 * A class for splitting a text file (via inputStream) into sequences that are
 * stored in different files based on the indexFile in outputDirectory.
 * 
 * @author Martin Koerner
 * 
 */
public class Sequencer {

    protected InputStream inputStream;

    protected File outputDirectory;

    protected WordIndex wordIndex;

    protected boolean[] pattern;

    protected String addBeforeSentence;

    protected String addAfterSentence;

    protected String delimiter;

    protected boolean completeLine;

    private int startSortAtColumn;

    Logger logger = LogManager.getLogger(this.getClass().getName());

    public Sequencer(
            InputStream inputStream,
            File outputDirectory,
            WordIndex wordIndex,
            boolean[] pattern,
            String addBeforeSentence,
            String addAfterSentence,
            String delimiter,
            boolean completeLine,
            int startSortAtColumn) {
        this.inputStream = inputStream;
        this.outputDirectory = outputDirectory;
        this.wordIndex = wordIndex;
        this.pattern = pattern;
        this.addBeforeSentence = addBeforeSentence;
        this.addAfterSentence = addAfterSentence;
        this.delimiter = delimiter;
        this.completeLine = completeLine;
        this.startSortAtColumn = startSortAtColumn;

    }

    public void splitIntoFiles() throws IOException {
        HashMap<Integer, BufferedWriter> writers =
                wordIndex.openWriters(outputDirectory);
        // TODO: bufferSize calculation
        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream),
                        100 * 8 * 1024);
        // BufferedReader bufferedReader = new BufferedReader(
        // new InputStreamReader(this.inputStream), 10 * 8 * 1024);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                line = addBeforeSentence + line + addAfterSentence;
                if (completeLine) {
                    String[] lineSplit = line.split("\\s");
                    writers.get(wordIndex.rank(lineSplit[startSortAtColumn]))
                            .write(line + "\n");
                } else {
                    String[] lineSplit = line.split("\\s");
                    int linePointer = 0;
                    while (lineSplit.length - linePointer >= pattern.length) {
                        String sequence = "";
                        for (int i = 0; i < pattern.length; i++) {
                            if (pattern[i]) {
                                sequence += lineSplit[linePointer + i] + " ";
                            }
                        }
                        sequence = sequence.replaceFirst(" $", "");
                        sequence += delimiter + "1\n";

                        // write sequence

                        writers.get(
                                wordIndex.rank(sequence.split(" ")[startSortAtColumn]))
                                .write(sequence);

                        linePointer++;
                    }
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        wordIndex.closeWriters(writers);
    }

    public boolean[] getPattern() {
        return pattern;
    }
}
