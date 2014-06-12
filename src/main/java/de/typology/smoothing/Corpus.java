package de.typology.smoothing;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.typology.counting.Counter;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public class Corpus {

    private Map<Pattern, Map<String, Integer>> absoluteCounts;

    private Map<Pattern, Map<String, Counter>> continuationCounts;

    private String delimiter;

    private static List<String> skippedList;
    static {
        skippedList = new ArrayList<>(1);
        skippedList.add(PatternElem.SKIPPED_WORD);
    }

    public Corpus(
            Path absoluteDir,
            Path continuationDir,
            String delimiter) throws IOException {
        this.delimiter = delimiter;
        absoluteCounts = readAbsoluteCounts(absoluteDir);
        continuationCounts = readContinuationCounts(continuationDir);
    }

    public Pattern getPattern(List<String> sequence) {
        List<PatternElem> patternElems =
                new ArrayList<PatternElem>(sequence.size());
        for (String word : sequence) {
            if (word.equals(PatternElem.SKIPPED_WORD)) {
                patternElems.add(PatternElem.SKP);
            } else {
                patternElems.add(PatternElem.CNT);
            }
        }
        return new Pattern(patternElems);
    }

    /**
     * Get the number of words in the corpus.
     * 
     * Aka absolute count of skip.
     */
    public long getNumWords() {
        return getAbsolute(skippedList);
    }

    /**
     * Get the vocabulary size (number of different words) of the corpus.
     * 
     * Aka continuation count of skip.
     */
    public long getVocabSize() {
        return getContinuation(skippedList).getOnePlusCount();
    }

    public int getAbsolute(List<String> sequence) {
        Pattern pattern = getPattern(sequence);
        String string = StringUtils.join(sequence, " ");
        Map<String, Integer> patternCounts = absoluteCounts.get(pattern);
        Integer count = patternCounts.get(string);
        return count == null ? 0 : count;
    }

    public Counter getContinuation(List<String> sequence) {
        if (sequence.isEmpty()) {
            return new Counter();
        }

        Pattern pattern =
                getPattern(sequence).replace(PatternElem.SKP, PatternElem.WSKP);
        String string = StringUtils.join(sequence, " ");
        Map<String, Counter> patternCounters = continuationCounts.get(pattern);
        if (patternCounters == null) {
            throw new NullPointerException(
                    "No continuation counts in corpus for pattern: " + pattern
                            + ".");
        }
        Counter counter = patternCounters.get(string);
        return counter == null ? new Counter() : counter;
    }

    private Map<Pattern, Map<String, Integer>> readAbsoluteCounts(
            Path absoluteDir) throws IOException {
        Map<Pattern, Map<String, Integer>> absoluteCounts =
                new HashMap<Pattern, Map<String, Integer>>();

        try (DirectoryStream<Path> absolutePatterns =
                Files.newDirectoryStream(absoluteDir)) {
            for (Path absolutePattern : absolutePatterns) {
                Pattern pattern =
                        new Pattern(absolutePattern.getFileName().toString());
                Map<String, Integer> counts = new HashMap<String, Integer>();
                absoluteCounts.put(pattern, counts);

                try (DirectoryStream<Path> files =
                        Files.newDirectoryStream(absolutePattern)) {
                    for (Path file : files) {
                        try (BufferedReader reader =
                                Files.newBufferedReader(file,
                                        Charset.defaultCharset())) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] split = line.split(delimiter);
                                String sequence = split[0];
                                int count = Integer.parseInt(split[1]);
                                counts.put(sequence, count);
                            }
                        }
                    }
                }
            }
        }

        return absoluteCounts;
    }

    private Map<Pattern, Map<String, Counter>> readContinuationCounts(
            Path continuationDir) throws IOException {
        Map<Pattern, Map<String, Counter>> continuationCounts =
                new HashMap<Pattern, Map<String, Counter>>();

        try (DirectoryStream<Path> continuationPatterns =
                Files.newDirectoryStream(continuationDir)) {
            for (Path continuationPattern : continuationPatterns) {
                Pattern pattern =
                        new Pattern(continuationPattern.getFileName()
                                .toString());
                Map<String, Counter> counts = new HashMap<String, Counter>();
                continuationCounts.put(pattern, counts);

                try (DirectoryStream<Path> files =
                        Files.newDirectoryStream(continuationPattern)) {
                    for (Path file : files) {
                        try (BufferedReader reader =
                                Files.newBufferedReader(file,
                                        Charset.defaultCharset())) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] split = line.split(delimiter);
                                String sequence = split[0];
                                long onePlusCount = Long.parseLong(split[1]);
                                long oneCount = Long.parseLong(split[2]);
                                long twoCount = Long.parseLong(split[3]);
                                long threePlusCount = Long.parseLong(split[4]);
                                Counter counter =
                                        new Counter(onePlusCount, oneCount,
                                                twoCount, threePlusCount);
                                counts.put(sequence, counter);
                            }
                        }
                    }
                }
            }
        }

        return continuationCounts;
    }

}
