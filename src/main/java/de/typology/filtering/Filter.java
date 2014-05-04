package de.typology.filtering;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.typology.patterns.PatternTransformer;

/**
 * Class used to interface with the output of {@link FilterBuilder}.
 */
public class Filter {

    private Path inputDirectory;

    private Map<boolean[], Set<String>> sequences;

    public Filter(
            Path inputDirectory) {
        this.inputDirectory = inputDirectory;
        sequences = new HashMap<boolean[], Set<String>>();
    }

    public boolean contains(String sequence, boolean[] pattern)
            throws IOException {
        loadPattern(pattern);

        return sequences.get(pattern).contains(sequence);
    }

    private void loadPattern(boolean[] pattern) throws IOException {
        if (!sequences.containsKey(pattern)) {
            String patternLabel = PatternTransformer.getStringPattern(pattern);
            Path patternDirectory = inputDirectory.resolve(patternLabel);

            if (!Files.isDirectory(patternDirectory)) {
                throw new IllegalStateException(
                        "No filter directory for pattern: " + patternDirectory
                                + ".");
            }

            Set<String> seqs = new HashSet<String>();

            try (DirectoryStream<Path> patternFiles =
                    Files.newDirectoryStream(patternDirectory)) {
                for (Path patternFile : patternFiles) {
                    try (BufferedReader reader =
                            Files.newBufferedReader(patternFile,
                                    Charset.defaultCharset())) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            seqs.add(line);
                        }
                    }
                }
            }

            sequences.put(pattern, seqs);
        }
    }
}
