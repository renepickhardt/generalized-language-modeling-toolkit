package de.glmtk.legacy.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.glmtk.pattern.Pattern;

/**
 * Class used to interface with the output of {@link IndexBuilder}.
 */
public class Index {

    private List<String> wordIndex = new ArrayList<String>();

    private List<String> posIndex = new ArrayList<String>();

    private boolean withPos;

    /**
     * Initialized new {@link Index}.
     * 
     * @param input
     *            {@link InputStream} to be read as the output of
     *            {@link IndexBuilder}.
     */
    public Index(
            InputStream input) throws IOException {
        // read the index file
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input))) {
            String line = reader.readLine();

            if (!line.equals("Words:")) {
                throw new IllegalStateException(
                        "Illegal File Format for index. Missing \"Words:\" section.");
            }

            withPos = false;
            while ((line = reader.readLine()) != null) {
                if (line.equals("Poses:")) {
                    withPos = true;
                    break;
                }
                wordIndex.add(line.split("\t")[0]);
            }

            if (withPos) {
                while ((line = reader.readLine()) != null) {
                    posIndex.add(line.split("\t")[0]);
                }
            }
        }
    }

    public IndexWriter openIndexWriter(
            Pattern pattern,
            Path outputDir,
            int bufferSizes) throws IOException {
        return new IndexWriter(this, pattern, outputDir, bufferSizes);
    }

    /* package */List<String> getWordIndex() {
        return wordIndex;
    }

    /* package */List<String> getPosIndex() {
        return posIndex;
    }

}
