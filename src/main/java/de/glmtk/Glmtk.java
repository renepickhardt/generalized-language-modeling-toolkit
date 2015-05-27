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

package de.glmtk;

import static com.google.common.hash.Hashing.md5;
import static com.google.common.io.Files.hash;
import static de.glmtk.output.Output.println;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import de.glmtk.common.Config;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.common.Status.Training;
import de.glmtk.counting.Chunker;
import de.glmtk.counting.LengthDistributionCalculator;
import de.glmtk.counting.Merger;
import de.glmtk.counting.NGramTimesCounter;
import de.glmtk.counting.Tagger;
import de.glmtk.logging.Logger;
import de.glmtk.output.Output;
import de.glmtk.output.ProgressBar;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.probability.FileQueryExecutor;
import de.glmtk.querying.probability.QueryCacherCreator;
import de.glmtk.querying.probability.QueryMode;
import de.glmtk.querying.probability.QueryStats;
import de.glmtk.querying.probability.StreamQueryExecutor;
import de.glmtk.util.NioUtils;

/**
 * Here happens counting (during training phase) and also querying (during
 * application phase) This class is been called either by one of the Executable
 * classes and filled from config or console input or it is called from
 * UnitTests Expects parameters to be set via setters before calling any other
 * method
 */
public class Glmtk {
    private static final Logger LOGGER = Logger.get(Glmtk.class);
    private static final String PHASE_ABSOLUTE_CHUNKING = "Chunking Absolute";
    private static final String PHASE_ABSOLUTE_MERGING = "Merging Absolute";
    private static final String PHASE_CONTINUATION_CHUNKING = "Chunking Continuation";
    private static final String PHASE_CONTINUATION_MERGING = "Merging Continuation";
    private static final String PHASE_NGRAM_TIMES_COUNTING = "Counting NGram Times";
    private static final String PHASE_LENGTH_DISTRIBUTION_MEASURING = "Measuring Length Distribution";

    private static class NeededComputations {
        private boolean tagging;
        private Set<Pattern> absolute;
        private Set<Pattern> continuation;

        public NeededComputations(boolean tagging,
                                  Set<Pattern> absolute,
                                  Set<Pattern> continuation) {
            this.tagging = tagging;
            this.absolute = absolute;
            this.continuation = continuation;
        }

        public boolean needTagging() {
            return tagging;
        }

        public Set<Pattern> getAbsolute() {
            return absolute;
        }

        public Set<Pattern> getContinuation() {
            return continuation;
        }
    }

    @SuppressWarnings("unused")
    private Config config;
    private Path corpus;
    private GlmtkPaths paths;
    private Status status;

    private Tagger tagger;
    private Chunker chunker;
    private Merger merger;
    private NGramTimesCounter ngramTimesCounter;
    private LengthDistributionCalculator lengthDistributionCalculator;

    private QueryCacherCreator queryCacherCreator;
    private StreamQueryExecutor streamQueryExecutor;
    private FileQueryExecutor fileQueryExecutor;

    public Glmtk(Config config,
                 Path corpus,
                 Path workingDir) throws Exception {
        this.config = config;
        this.corpus = corpus;

        paths = new GlmtkPaths(workingDir);
        paths.logPaths();

        Files.createDirectories(workingDir);

        status = new Status(paths, corpus);
        status.logStatus();

        tagger = new Tagger(config);
        chunker = new Chunker(config);
        merger = new Merger(config);
        ngramTimesCounter = new NGramTimesCounter(config);
        lengthDistributionCalculator = new LengthDistributionCalculator(config);

        queryCacherCreator = new QueryCacherCreator(config);
        streamQueryExecutor = new StreamQueryExecutor(config);
        fileQueryExecutor = new FileQueryExecutor(config);
    }

    public GlmtkPaths getPaths() {
        return paths;
    }

    public Status getStatus() {
        return status;
    }

    public void count(Set<Pattern> neededPatterns) throws Exception {
        NeededComputations needed = computeNeeded(neededPatterns);

        provideTraining(needed.needTagging());

        println("Corpus analyzation...");

        ProgressBar progressBar = new ProgressBar(PHASE_ABSOLUTE_CHUNKING,
                PHASE_ABSOLUTE_MERGING, PHASE_CONTINUATION_CHUNKING,
                PHASE_CONTINUATION_MERGING, PHASE_NGRAM_TIMES_COUNTING,
                PHASE_LENGTH_DISTRIBUTION_MEASURING);

        countAbsolute(needed.getAbsolute(), progressBar);
        countContinuation(needed.getContinuation(), progressBar);

        progressBar.setPhase(PHASE_NGRAM_TIMES_COUNTING);
        ngramTimesCounter.count(status, paths.getNGramTimesFile(),
                paths.getAbsoluteDir(), paths.getContinuationDir(), progressBar);

        progressBar.setPhase(PHASE_LENGTH_DISTRIBUTION_MEASURING);
        lengthDistributionCalculator.calculate(status, paths.getTrainingFile(),
                paths.getLengthDistributionFile(), progressBar);

        long corpusSize = NioUtils.calcFileSize(paths.getCountsDir());
        println("    Saved as '%s' (uses %s).", Output.bold(paths.getDir()),
                humanReadableByteCount(corpusSize));
    }

    private NeededComputations computeNeeded(Set<Pattern> neededPatterns) {
        boolean tagging = false;
        Set<Pattern> absolute = new HashSet<>();
        Set<Pattern> continuation = new HashSet<>();

        Queue<Pattern> queue = new LinkedList<>(neededPatterns);
        while (!queue.isEmpty()) {
            Pattern pattern = queue.poll();
            if (pattern.isPos())
                tagging = true;
            if (pattern.isAbsolute())
                absolute.add(pattern);
            else {
                continuation.add(pattern);
                Pattern source = pattern.getContinuationSource();
                if ((source.isAbsolute() ? absolute : continuation).add(source))
                    queue.add(source);
            }
        }

        LOGGER.debug("needPos            = %s", tagging);
        LOGGER.debug("neededAbsolute     = %s", absolute);
        LOGGER.debug("neededContinuation = %s", continuation);

        return new NeededComputations(tagging, absolute, continuation);
    }

    private void provideTraining(boolean needTagging) throws IOException {
        if (status.getTraining() == Training.TAGGED) {
            LOGGER.info("Detected tagged training already present.");
            return;
        }

        Path trainingFile = paths.getTrainingFile();
        if (!needTagging) {
            if (status.getTraining() == Training.UNTAGGED) {
                LOGGER.info("Detected training already present");
                return;
            }

            if (!corpus.equals(trainingFile)) {
                Files.deleteIfExists(trainingFile);
                Files.copy(corpus, trainingFile);
            }
            if (status.isCorpusTagged())
                status.setTraining(Training.TAGGED);
            else
                status.setTraining(Training.UNTAGGED);
        } else {
            if (status.isCorpusTagged()) {
                if (!corpus.equals(trainingFile)) {
                    Files.deleteIfExists(trainingFile);
                    Files.copy(corpus, trainingFile);
                }
            } else {
                Path untaggedTrainingFile = paths.getUntaggedTrainingFile();
                Files.deleteIfExists(untaggedTrainingFile);
                Files.copy(corpus, untaggedTrainingFile);
                Files.deleteIfExists(trainingFile);

                tagger.tag(untaggedTrainingFile, trainingFile);
            }
            status.setTraining(Training.TAGGED);
        }
    }

    private void countAbsolute(Set<Pattern> neededPatterns,
                               ProgressBar progressBar) throws Exception {
        LOGGER.info("Absolute counting '%s' -> '%s'.", paths.getTrainingFile(),
                paths.getAbsoluteDir());

        Set<Pattern> countingPatterns = new HashSet<>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(true));

        Set<Pattern> chunkingPatterns = new HashSet<>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(true));

        LOGGER.debug("neededPatterns   = %s", neededPatterns);
        LOGGER.debug("countingPatterns = %s", countingPatterns);
        LOGGER.debug("chunkingPatterns = %s", chunkingPatterns);

        progressBar.setPhase(PHASE_ABSOLUTE_CHUNKING);
        chunker.chunkAbsolute(status, chunkingPatterns,
                paths.getTrainingFile(),
                status.getTraining() == Training.TAGGED,
                paths.getAbsoluteChunkedDir(), progressBar);
        validateExpectedResults("Absolute chunking", chunkingPatterns,
                status.getChunkedPatterns(true));

        progressBar.setPhase(PHASE_ABSOLUTE_MERGING);
        merger.mergeAbsolute(status, countingPatterns,
                paths.getAbsoluteChunkedDir(), paths.getAbsoluteDir(),
                progressBar);
        validateExpectedResults("Absolute counting", countingPatterns,
                status.getCounted(true));
    }

    private void countContinuation(Set<Pattern> neededPatterns,
                                   ProgressBar progressBar) throws Exception {
        LOGGER.info("Continuation counting '%s' -> '%s'.",
                paths.getAbsoluteDir(), paths.getContinuationDir());

        Set<Pattern> countingPatterns = new HashSet<>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(false));

        Set<Pattern> chunkingPatterns = new HashSet<>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(false));

        LOGGER.debug("neededPatterns   = %s", neededPatterns);
        LOGGER.debug("countingPatterns = %s", countingPatterns);
        LOGGER.debug("chunkingPatterns = %s", chunkingPatterns);

        progressBar.setPhase(PHASE_CONTINUATION_CHUNKING);
        chunker.chunkContinuation(status, chunkingPatterns,
                paths.getAbsoluteDir(), paths.getContinuationDir(),
                paths.getAbsoluteChunkedDir(),
                paths.getContinuationChunkedDir(), progressBar);
        validateExpectedResults("Continuation chunking", chunkingPatterns,
                status.getChunkedPatterns(false));

        progressBar.setPhase(PHASE_CONTINUATION_MERGING);
        merger.mergeContinuation(status, countingPatterns,
                paths.getContinuationChunkedDir(), paths.getContinuationDir(),
                progressBar);
        validateExpectedResults("Continuation counting", countingPatterns,
                status.getCounted(false));
    }

    public GlmtkPaths provideQueryCache(Path queryFile,
                                        Set<Pattern> patterns) throws Exception {
        println("QueryCache for file '%s'...", queryFile);

        String hash = hash(queryFile.toFile(), md5()).toString();

        Set<Pattern> neededPatterns = new HashSet<>(patterns);
        neededPatterns.removeAll(status.getQueryCacheCounted(hash));

        boolean tagged = Tagger.detectFileTagged(queryFile);
        GlmtkPaths queryCachePaths = queryCacherCreator.createQueryCache(hash,
                queryFile, tagged, neededPatterns, status, paths);
        validateExpectedResults("Caching pattern counts", neededPatterns,
                status.getQueryCacheCounted(hash));

        Path dir = queryCachePaths.getDir();
        long size = NioUtils.calcFileSize(dir);
        println("    Saved as '%s' under '%s' (uses %s).", dir.getFileName(),
                dir.getParent(), humanReadableByteCount(size));

        return queryCachePaths;
    }

    public QueryStats queryStream(QueryMode mode,
                                  Estimator estimator,
                                  int corpusOrder,
                                  InputStream inputStream,
                                  OutputStream outputStream) throws IOException {
        return streamQueryExecutor.queryStream(paths, mode, estimator,
                corpusOrder, inputStream, outputStream);
    }

    public QueryStats queryFile(QueryMode mode,
                                Estimator estimator,
                                int corpusOrder,
                                Path inputFile) throws Exception {
        Files.createDirectories(paths.getQueriesDir());

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        Path outputFile = paths.getQueriesDir().resolve(
                String.format("%s %s %s %s", inputFile.getFileName(),
                        estimator, mode, date));

        return queryFile(mode, estimator, corpusOrder, inputFile, outputFile);
    }

    public QueryStats queryFile(QueryMode mode,
                                Estimator estimator,
                                int corpusOrder,
                                Path inputFile,
                                Path outputFile) throws Exception {
        return fileQueryExecutor.queryFile(paths, mode, estimator, corpusOrder,
                inputFile, outputFile);
    }

    /**
     * Only used internally.
     */
    public static void validateExpectedResults(String operation,
                                               Set<Pattern> expected,
                                               Set<Pattern> computed) {
        if (!computed.containsAll(expected)) {
            Set<Pattern> missing = new HashSet<>();
            missing.addAll(expected);
            missing.removeAll(computed);

            LOGGER.error("%s did not yield expected result.%n", operation);
            LOGGER.error("Expected patterns = %s.%n", expected);
            LOGGER.error("Computed patterns = %s.%n", computed);
            LOGGER.error("Missing  patterns = %s.", missing);
            throw new RuntimeException("%s failed.");
        }
    }
}
