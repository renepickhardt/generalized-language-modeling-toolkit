/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.common;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.common.PatternElem.CNT;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.counts.Counts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.files.CountsReader;
import de.glmtk.files.LengthDistributionReader;
import de.glmtk.files.NGramTimesReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.CollectionUtils;

/**
 * Tests for this class can be found in {@link de.glmtk.counting.CountingTest}.
 */
public class CountCache {
    private static final Logger LOGGER = Logger.get(CountCache.class);

    private static Set<Pattern> getAvailablePatternsFromFilesystem(GlmtkPaths paths) throws IOException {
        Set<Pattern> patterns = new HashSet<>();
        try (DirectoryStream<Path> absoluteDirStream = Files.newDirectoryStream(paths.getAbsoluteDir())) {
            for (Path patternFile : absoluteDirStream)
                patterns.add(Patterns.get(patternFile.getFileName().toString()));
        }
        try (DirectoryStream<Path> continuationDirStream = Files.newDirectoryStream(paths.getContinuationDir())) {
            for (Path patternFile : continuationDirStream)
                patterns.add(Patterns.get(patternFile.getFileName().toString()));
        }
        return patterns;
    }

    private Progress progress;
    private Map<Pattern, Map<String, Long>> absolute;
    private Map<Pattern, Map<String, Counts>> continuation;
    private Map<Pattern, NGramTimes> nGramTimes;
    private List<Double> lengthFrequencies;

    public CountCache(Set<Pattern> patterns,
                      GlmtkPaths paths) throws IOException {
        // Allowing arguments == null to make
        // {@link Patterns#getUsedPatterns(ParamEstimator, ProbMode)} work.
        if (patterns == null || paths == null)
            return;

        String message = "Loading counts into memory";
        OUTPUT.beginPhases(message + "...");
        OUTPUT.setPhase(Phase.LOADING_CACHE);

        progress = OUTPUT.newProgress(patterns.size() + 2);

        loadCounts(paths.getAbsoluteDir(), paths.getContinuationDir(), patterns);
        loadNGramTimes(paths.getNGramTimesFile());
        loadLengthDistribution(paths.getLengthDistributionFile());

        OUTPUT.endPhases(message + " done.");
    }

    /**
     * Debugging constructor, that just loads all patterns in folder into
     * memory.
     */
    public CountCache(GlmtkPaths paths) throws Exception {
        this(getAvailablePatternsFromFilesystem(paths), paths);
    }

    public long getAbsolute(NGram sequence) {
        Map<String, Long> countsWithPattern = absolute.get(sequence.getPattern());
        if (countsWithPattern == null)
            throw new IllegalStateException(String.format(
                    "No absolute counts learned for pattern: '%s'.",
                    sequence.getPattern()));
        Long count = countsWithPattern.get(sequence.toString());
        return count == null ? 0 : count;
    }

    public Counts getContinuation(NGram sequence) {
        Map<String, Counts> countsWithPattern = continuation.get(sequence.getPattern());
        if (countsWithPattern == null)
            throw new IllegalStateException(String.format(
                    "No continuation counts learned for pattern: '%s'.",
                    sequence.getPattern()));
        Counts counts = countsWithPattern.get(sequence.toString());
        return counts == null ? new Counts() : counts;
    }

    public NGramTimes getNGramTimes(Pattern pattern) {
        NGramTimes counts = nGramTimes.get(pattern);
        if (counts == null)
            throw new IllegalStateException(
                    String.format(
                            "No ngram times counts learned for pattern: '%s'.",
                            pattern));
        return counts;
    }

    public double getLengthFrequency(int length) {
        if (length < 1)
            throw new IllegalArgumentException(
                    String.format(
                            "Illegal sequence length requested: '%d'. Must be a positive integer.",
                            length));
        if (length >= lengthFrequencies.size())
            return 0.0;
        return lengthFrequencies.get(length);
    }

    public int getMaxSequenceLength() {
        return lengthFrequencies.size() + 1;
    }

    public long getNumWords() {
        return getAbsolute(SKP_NGRAM);
    }

    public long getVocabSize() {
        return getContinuation(WSKP_NGRAM).getOnePlusCount();
    }

    public SortedSet<String> getWords() {
        return new TreeSet<>(absolute.get(Patterns.get(CNT)).keySet());
    }

    private void loadCounts(Path absoluteDir,
                            Path continuationDir,
                            Set<Pattern> patterns) throws IOException {
        LOGGER.debug("Loading counts...");
        absolute = new HashMap<>();
        continuation = new HashMap<>();

        for (Pattern pattern : patterns) {
            boolean isPatternAbsolute;
            Path inputDir;
            Map<String, Long> absoluteCounts = null;
            Map<String, Counts> continuationCounts = null;
            if (pattern.isAbsolute()) {
                isPatternAbsolute = true;
                inputDir = absoluteDir;
                absoluteCounts = new HashMap<>();
                absolute.put(pattern, absoluteCounts);
            } else {
                isPatternAbsolute = false;
                inputDir = continuationDir;
                continuationCounts = new HashMap<>();
                continuation.put(pattern, continuationCounts);
            }

            Path file = inputDir.resolve(pattern.toString());
            try (CountsReader reader = new CountsReader(file, Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String sequence = reader.getSequence();
                    Counts counts = reader.getCounts();
                    if (isPatternAbsolute)
                        absoluteCounts.put(sequence, counts.getOnePlusCount());
                    else
                        continuationCounts.put(sequence, counts);
                }
            }

            progress.increase(1);
        }
    }

    private void loadNGramTimes(Path file) throws IOException {
        LOGGER.debug("Loading NGram times counts...");
        nGramTimes = new HashMap<>();
        try (NGramTimesReader reader = new NGramTimesReader(file,
                Constants.CHARSET)) {
            while (reader.readLine() != null)
                nGramTimes.put(reader.getPattern(), reader.getNGramTimes());
        }
        progress.increase(1);
    }

    private void loadLengthDistribution(Path file) throws IOException {
        LOGGER.debug("Loading Sequence Length Distribution...");
        lengthFrequencies = new ArrayList<>();
        try (LengthDistributionReader reader = new LengthDistributionReader(
                file, Constants.CHARSET)) {
            while (reader.readLine() != null) {
                int length = reader.getLength();
                double frequency = reader.getFrequency();

                CollectionUtils.ensureListSize(lengthFrequencies, length, 0.0);
                lengthFrequencies.set(length, frequency);
            }
        }
        progress.increase(1);
    }
}
