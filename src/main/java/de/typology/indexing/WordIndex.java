package de.typology.indexing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Class used to interface with the output of {@link WordIndexBuilder}.
 */
public class WordIndex {

    private List<String> index = new ArrayList<String>();

    /**
     * Initialized new {@link WordIndex}.
     * 
     * @param input
     *            {@link InputStream} to be read as the output of
     *            {@link WordIndexBuilder}.
     */
    public WordIndex(
            InputStream input) throws IOException {
        // read the index file
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                index.add(line.split("\t")[0]);
            }
        }
    }

    /**
     * Returns the file in which {@code word} should be stored based on the
     * index.
     * 
     * Performs binary search on the index.
     * 
     * @param word
     *            The word to be stored.
     * @return An integer representing the <em>indexed file</em> the word should
     *         be stored in.
     */
    public int rank(String word) {
        int lo = 0;
        int hi = index.size() - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (word.compareTo(index.get(mid)) < 0) {
                hi = mid - 1;
            } else if (word.compareTo(index.get(mid)) > 0) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        // the following return statement is not the standard return result for
        // binary search
        return (lo + hi) / 2;
    }

    public List<BufferedWriter> openWriters(Path outputDir) throws IOException {
        List<BufferedWriter> writers =
                new ArrayList<BufferedWriter>(index.size());

        // TODO: research why directories are written multiple times to.
        if (Files.exists(outputDir)) {
            // TODO: replace with non legacy api.
            FileUtils.deleteDirectory(outputDir.toFile());
        }
        Files.createDirectory(outputDir);

        for (Integer i = 0; i != index.size(); ++i) {
            writers.add(new BufferedWriter(new OutputStreamWriter(Files
                    .newOutputStream(outputDir.resolve(i.toString()))),
                    10 * 8 * 1024));
        }

        return writers;
    }

    public void closeWriters(List<BufferedWriter> writers) throws IOException {
        for (BufferedWriter writer : writers) {
            writer.close();
        }
    }

}
