package de.glmtk.utils;

import static de.glmtk.utils.NGram.SKP_NGRAM;
import static de.glmtk.utils.NGram.WSKP_NGRAM;
import static de.glmtk.utils.PatternElem.CNT;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tests for this class can be found in {@link CountingTest}.
 */
public class CountCache {

    private static final Logger LOGGER = LogManager.getLogger(CountCache.class);

    private Map<Pattern, Map<String, Long>> absolute =
            new HashMap<Pattern, Map<String, Long>>();

    private Map<Pattern, Map<String, Counter>> continuation =
            new HashMap<Pattern, Map<String, Counter>>();

    private Map<Pattern, long[]> nGramTimes = new HashMap<Pattern, long[]>();

    public CountCache(
            Path workingDir) throws IOException {
        // Allowing workingDir == null to make
        // PatternCalculator#PatternTrackingCountCache work.
        if (workingDir == null) {
            return;
        }

        LOGGER.info("Loading counts...");
        LOGGER.debug("Loading absolute counts...");
        loadAbsolute(workingDir);
        LOGGER.debug("Loading continuation counts...");
        loadContinuation(workingDir);
        LOGGER.debug("Loading nGramTimes counts...");
        loadNGramTimes();
    }

    private void loadAbsolute(Path workingDir) throws IOException {
        try (DirectoryStream<Path> files =
                Files.newDirectoryStream(workingDir.resolve("absolute"))) {
            for (Path file : files) {
                Pattern pattern = Pattern.get(file.getFileName().toString());
                Map<String, Long> counts = new HashMap<String, Long>();
                absolute.put(pattern, counts);

                try (BufferedReader reader =
                        Files.newBufferedReader(file, Charset.defaultCharset())) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Counter counter = new Counter();
                        String sequence =
                                Counter.getSequenceAndCounter(line, counter);
                        counts.put(sequence, counter.getOnePlusCount());
                    }
                }
            }
        }
    }

    private void loadContinuation(Path workingDir) throws IOException {
        try (DirectoryStream<Path> files =
                Files.newDirectoryStream(workingDir.resolve("continuation"))) {
            for (Path file : files) {
                Pattern pattern = Pattern.get(file.getFileName().toString());
                Map<String, Counter> counts = new HashMap<String, Counter>();
                continuation.put(pattern, counts);

                try (BufferedReader reader =
                        Files.newBufferedReader(file, Charset.defaultCharset())) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Counter counter = new Counter();
                        String sequence =
                                Counter.getSequenceAndCounter(line, counter);
                        counts.put(sequence, counter);
                    }
                }
            }
        }
    }

    private void loadNGramTimes() {
        for (Pattern pattern : absolute.keySet()) {
            long[] counts = {
                0L, 0L, 0L, 0L
            };

            for (long count : absolute.get(pattern).values()) {
                if (count == 0 || count > 4) {
                    continue;
                }
                ++counts[(int) count - 1];
            }

            nGramTimes.put(pattern, counts);
        }
    }

    public long getAbsolute(NGram sequence) {
        Map<String, Long> counts = absolute.get(sequence.getPattern());
        if (counts == null) {
            throw new IllegalStateException(
                    "No absolute counts learned for pattern: '"
                            + sequence.getPattern() + "'.");
        }
        Long count = counts.get(sequence.toString());
        return count == null ? 0 : count;
    }

    public Counter getContinuation(NGram sequence) {
        Map<String, Counter> counts = continuation.get(sequence.getPattern());
        if (counts == null) {
            throw new IllegalStateException(
                    "No continuation counts learned for pattern: '"
                            + sequence.getPattern() + "'.");
        }
        Counter counter = counts.get(sequence.toString());
        return counter == null ? new Counter() : counter;
    }

    public long[] getNGramTimes(Pattern pattern) {
        long[] counts = nGramTimes.get(pattern);
        if (counts == null) {
            throw new IllegalStateException(
                    "No nGramTimes counts learned for pattern'" + pattern
                    + "'.");
        }
        return counts;
    }

    public long getNumWords() {
        return getAbsolute(SKP_NGRAM);
    }

    public long getVocabSize() {
        return getContinuation(WSKP_NGRAM).getOnePlusCount();
    }

    public SortedSet<String> getWords() {
        return new TreeSet<String>(absolute.get(Pattern.get(CNT)).keySet());
    }

}
