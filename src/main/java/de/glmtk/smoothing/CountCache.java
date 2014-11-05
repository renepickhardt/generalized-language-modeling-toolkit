package de.glmtk.smoothing;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.glmtk.Counter;
import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;
import de.glmtk.utils.StringUtils;

public class CountCache {

    private Map<Pattern, Map<Integer, Integer>> nGramTimesCountCache =
            new HashMap<Pattern, Map<Integer, Integer>>();

    private Map<Pattern, Map<String, Integer>> absoluteCounts;

    private Map<Pattern, Map<String, Counter>> continuationCounts;

    public static final List<String> SKIPPED_LIST;
    static {
        SKIPPED_LIST = new ArrayList<String>(1);
        SKIPPED_LIST.add(PatternElem.SKIPPED_WORD);
    }

    public static final Pattern CNT_PATTERN = new Pattern(
            Arrays.asList(PatternElem.CNT));

    public CountCache(
            Path workingDir) throws IOException {
        Path absoluteDir = workingDir.resolve("absolute");
        Path continuationDir = workingDir.resolve("continuation");
        absoluteCounts = readAbsoluteCounts(absoluteDir);
        continuationCounts = readContinuationCounts(continuationDir);
    }

    /**
     * Get the number of words in the corpus.
     *
     * Aka absolute count of skip.
     */
    public int getNumWords() {
        return getAbsolute(SKIPPED_LIST);
    }

    /**
     * Get the vocabulary size (number of different words) of the corpus.
     *
     * Aka continuation count of skip.
     */
    public long getVocabSize() {
        return getContinuation(SKIPPED_LIST).getOnePlusCount();
    }

    public SortedSet<String> getWords() {
        return new TreeSet<String>(absoluteCounts.get(CNT_PATTERN).keySet());
    }

    /**
     * @return The total number of n-grams with {@code pattern} which appear
     *         exactly {@code times} often in the training data.
     */
    public int getNGramTimesCount(Pattern pattern, int times) {
        // TODO: check if is getOneCount from ContinuationCounts.
        Map<Integer, Integer> patternCache = nGramTimesCountCache.get(pattern);
        if (patternCache == null) {
            patternCache = new HashMap<Integer, Integer>();
            nGramTimesCountCache.put(pattern, patternCache);
        }

        Integer count = patternCache.get(times);
        if (count == null) {
            count = 0;
            for (int absoluteCount : absoluteCounts.get(pattern).values()) {
                if (absoluteCount == times) {
                    ++count;
                }
            }
            patternCache.put(times, count);
        }

        return count;
    }

    public int getAbsolute(NGram sequence) {
        return getAbsolute(sequence.toList());
    }

    public int getAbsolute(List<String> sequence) {
        Pattern pattern = getPattern(sequence);
        String string = StringUtils.join(sequence, " ");
        Map<String, Integer> patternCounts = absoluteCounts.get(pattern);
        Integer count = patternCounts.get(string);
        return count == null ? 0 : count;
    }

    public Counter getContinuation(NGram sequence) {
        return getContinuation(sequence.toList());
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

    private Pattern getPattern(List<String> sequence) {
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

                try (BufferedReader reader =
                        Files.newBufferedReader(absolutePattern,
                                Charset.defaultCharset())) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] split =
                                StringUtils.splitAtChar(line, '\t').toArray(
                                        new String[0]);
                        String sequence = split[0];
                        int count = Integer.parseInt(split[1]);
                        counts.put(sequence, count);
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

                try (BufferedReader reader =
                        Files.newBufferedReader(continuationPattern,
                                Charset.defaultCharset())) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] split =
                                StringUtils.splitAtChar(line, '\t').toArray(
                                        new String[0]);
                        String sequence = split[0];
                        long onePlusCount = Long.parseLong(split[1]);
                        long oneCount = Long.parseLong(split[2]);
                        long twoCount = Long.parseLong(split[3]);
                        long threePlusCount = Long.parseLong(split[4]);
                        Counter counter =
                                new Counter(onePlusCount, oneCount, twoCount,
                                        threePlusCount);
                        counts.put(sequence, counter);
                    }
                }
            }
        }

        return continuationCounts;
    }
}
