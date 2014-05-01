package de.typology.indexes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

/**
 * A class that is based on the text file produced by WordIndexer.
 * 
 * @author Martin Koerner
 * 
 */
public class WordIndex implements Iterable<String> {

    private String[] index;

    public WordIndex(
            File indexFile) throws IOException {
        // count total number of lines in the index file
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(indexFile))) {
            while (br.readLine() != null) {
                lineCount++;
            }
        }

        index = new String[lineCount];
        int currentLineCount = 0;

        // read the index file
        try (BufferedReader br = new BufferedReader(new FileReader(indexFile))) {
            String line;
            String[] lineSplit;
            while ((line = br.readLine()) != null) {
                lineSplit = line.split("\t");
                index[currentLineCount] = lineSplit[0];
                currentLineCount++;
            }
        }
    }

    public int getLength() {
        return index.length;
    }

    /**
     * returns the file in which word should be stored based on this.index
     * 
     * @param word
     * @return
     */
    public int rank(String word) {
        int lo = 0;
        int hi = index.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (word.compareTo(index[mid]) < 0) {
                hi = mid - 1;
            } else if (word.compareTo(index[mid]) > 0) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        // the following return statement is not the standard return result for
        // binary search
        return (lo + hi) / 2;
    }

    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(index).iterator();
    }

    public HashMap<Integer, BufferedWriter> openWriters(Path outputDirectory)
            throws IOException {
        HashMap<Integer, BufferedWriter> writers =
                new HashMap<Integer, BufferedWriter>();

        // TODO: research why we directories are written multiple times to.
        if (Files.exists(outputDirectory)) {
            // TODO: replace with non legacy api.
            FileUtils.deleteDirectory(outputDirectory.toFile());
        }
        Files.createDirectory(outputDirectory);

        // calculate buffer size for writers
        // TODO: bufferSize calculation
        for (int fileCount = 0; fileCount < index.length; fileCount++) {
            writers.put(fileCount, new BufferedWriter(new FileWriter(
                    outputDirectory + "/" + fileCount), 10 * 8 * 1024));
        }
        return writers;
    }

    public void closeWriters(HashMap<Integer, BufferedWriter> writers)
            throws IOException {
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
    }
}
