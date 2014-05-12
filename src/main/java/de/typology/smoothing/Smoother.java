package de.typology.smoothing;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.typology.counting.Counter;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public abstract class Smoother {

    protected Map<Pattern, Map<String, Integer>> absoluteCounts;

    protected Map<Pattern, Map<String, Counter>> continuationCounts;

    private String delimiter;

    public Smoother(
            Path absoluteDir,
            Path continuationDir,
            String delimiter) throws IOException {
        this.delimiter = delimiter;
        absoluteCounts = readAbsoluteCounts(absoluteDir);
        continuationCounts = readContinuationCounts(continuationDir);
    }

    public double propability(String sequence) {
        List<String> words = StringUtils.splitAtSpace(sequence);

        double result = 1;
        for (int i = 0; i != words.size(); ++i) {
            String word = words.get(i);
            List<String> givenSequence =
                    new LinkedList<String>(words.subList(0, i));
            for (int j = 0; j != words.size() - i - 1; ++j) {
                givenSequence.add(PatternElem.SKIPPED_WORD);
            }

            result *= propability_given(word, givenSequence);
        }
        return result;
    }

    protected abstract double propability_given(
            String word,
            List<String> givenSequence);

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
