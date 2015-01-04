package de.glmtk;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.counting.Chunker.CHUNKER;
import static de.glmtk.counting.Merger.MERGER;
import static de.glmtk.counting.Tagger.TAGGER;
import static de.glmtk.util.NioUtils.CheckFile.EXISTS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status.Training;
import de.glmtk.common.CountCache;
import de.glmtk.common.Counter;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.ProbMode;
import de.glmtk.counting.LengthDistribution;
import de.glmtk.querying.Query;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.HashUtils;
import de.glmtk.util.NioUtils;
import de.glmtk.util.PrintUtils;
import de.glmtk.util.StringUtils;

/**
 * Here happens counting (during training phase) and also querying (during
 * application phase) This class is been called either by one of the Executable
 * classes and filled from config or console input or it is called from
 * UnitTests Expects parameters to be set via setters before calling any other
 * method TODO: what about default parameters
 */
public class Glmtk {
    // TODO: API bugs with spaces in filenames
    // TODO: fix needPos (reduntant parameter)
    // TODO: Output should be empty if a phase is skipped
    // TODO: Some Unicode bug prevents "海底軍艦 , to be Undersea" from turning up in en0008t corpus absolute 11111 counts.
    // TODO: Detect ngram model length from testing.
    // TODO: only count nGramTimes if needed
    // TODO: enable comment syntax in input files
    // TODO: how is testing file input treated? (empty lines?)
    // TODO: verify that training does not contain any reserved symbols (_ % / multiple spaces)
    // TODO: update status with smaller increments (each completed pattern).

    private static final Logger LOGGER = LogManager.getFormatterLogger(Glmtk.class);

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

    private Path corpus;
    private GlmtkPaths paths;
    private Status status;
    private CountCache countCache = null;

    public Glmtk(Path corpus,
                 Path workingDir) throws Exception {
        this.corpus = corpus;

        paths = new GlmtkPaths(workingDir);

        Files.createDirectories(workingDir);

        status = new Status(paths, corpus);
        status.logStatus();
        // TODO: check file system if status is accurate.
    }

    public GlmtkPaths getPaths() {
        return paths;
    }

    public void count(Set<Pattern> neededPatterns) throws Exception {
        NeededComputations needed = computeNeeded(neededPatterns);

        provideTraining(needed.needTagging());

        OUTPUT.beginPhases("Corpus Analyzation...");
        countAbsolute(needed.getAbsolute());
        countContinuation(needed.getContinuation());

        OUTPUT.setPhase(Phase.EVALUATING, true);
        Progress progress = new Progress(2);

        // N-Gram Times Counts
        LOGGER.info("nGramTimes counting -> '%s'.", paths.getNGramTimesFile());
        try (BufferedWriter writer = Files.newBufferedWriter(
                paths.getNGramTimesFile(), Constants.CHARSET);
                DirectoryStream<Path> absoluteFiles = Files.newDirectoryStream(paths.getAbsoluteDir())) {
            for (Path absoluteFile : absoluteFiles) {
                long[] nGramTimes = {0L, 0L, 0L, 0L};

                try (BufferedReader reader = Files.newBufferedReader(
                        absoluteFile, Constants.CHARSET)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Counter counter = new Counter();
                        Counter.getSequenceAndCounter(line, counter);

                        // downcast is ok here
                        int count = (int) counter.getOnePlusCount();

                        if (count == 0 || count > 4)
                            continue;
                        ++nGramTimes[count - 1];
                    }
                }

                writer.write(absoluteFile.getFileName().toString());
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[0]));
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[1]));
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[2]));
                writer.write('\t');
                writer.write(Long.toString(nGramTimes[3]));
                writer.write('\n');
            }
        }
        progress.increase(1);

        // Sequence Length Distribution
        if (!NioUtils.checkFile(paths.getLengthDistributionFile(), EXISTS)) {
            LengthDistribution lengthDistribution = new LengthDistribution(
                    paths.getTrainingFile(), true);
            lengthDistribution.writeToStore(paths.getLengthDistributionFile());
        }
        progress.increase(1);

        long corpusSize = NioUtils.calcFileSize(Arrays.asList(paths.getCountsDir()));
        OUTPUT.endPhases(String.format("Corpus Analyzation done (uses %s).",
                PrintUtils.humanReadableByteCount(corpusSize)));
    }

    private NeededComputations computeNeeded(Set<Pattern> neededPatterns) {
        boolean tagging = false;
        Set<Pattern> absolute = new HashSet<Pattern>();
        Set<Pattern> continuation = new HashSet<Pattern>();

        Queue<Pattern> queue = new LinkedList<Pattern>(neededPatterns);
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
        // TODO: Need to check if training is already tagged and act accordingly.
        // TODO: doesn't detect the setting that user changed from untagged training file, to tagged file with same corpus.
        // TODO: doesn't detect when switching from untagged training to continuing with now tagged corpus.

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

                TAGGER.tag(untaggedTrainingFile, trainingFile);
            }
            status.setTraining(Training.TAGGED);
        }
    }

    private void countAbsolute(Set<Pattern> neededPatterns) throws Exception {
        LOGGER.info("Absolute counting '%s' -> '%s'.", paths.getTrainingFile(),
                paths.getAbsoluteDir());

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(false));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(false));

        LOGGER.debug("neededPatterns   = %s", neededPatterns);
        LOGGER.debug("countingPatterns = %s", countingPatterns);
        LOGGER.debug("chunkingPatterns = %s", chunkingPatterns);

        CHUNKER.chunkAbsolute(chunkingPatterns, status,
                paths.getTrainingFile(), paths.getAbsoluteChunkedDir());
        validateExpectedResults(false, false, chunkingPatterns);

        MERGER.mergeAbsolute(status, countingPatterns,
                paths.getAbsoluteChunkedDir(), paths.getAbsoluteDir());
        validateExpectedResults(false, true, countingPatterns);
    }

    private void countContinuation(Set<Pattern> neededPatterns) throws Exception {
        LOGGER.info("Continuation counting '%s' -> '%s'.",
                paths.getAbsoluteDir(), paths.getContinuationDir());

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(true));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(true));

        LOGGER.debug("neededPatterns   = %s", neededPatterns);
        LOGGER.debug("countingPatterns = %s", countingPatterns);
        LOGGER.debug("chunkingPatterns = %s", chunkingPatterns);

        CHUNKER.chunkContinuation(chunkingPatterns, status,
                paths.getAbsoluteDir(), paths.getAbsoluteChunkedDir(),
                paths.getContinuationDir(), paths.getContinuationChunkedDir());
        validateExpectedResults(true, false, chunkingPatterns);

        MERGER.mergeContinuation(status, countingPatterns,
                paths.getContinuationChunkedDir(), paths.getContinuationDir());
        validateExpectedResults(true, true, countingPatterns);
    }

    private void validateExpectedResults(boolean continuation,
                                         boolean counting,
                                         Set<Pattern> expected) throws Exception {
        Set<Pattern> computed = !counting
                ? status.getChunkedPatterns(continuation)
                : status.getCounted(continuation);
                if (!computed.containsAll(expected)) {
                    String continuationStr = !continuation
                    ? "Absolute"
                    : "Continuation";
                    String countingStr = !counting ? "chunking" : "counting";
                    throw new Exception(
                            String.format(
                                    "%s %s did not yield expected result.\n"
                                            + "Expected patterns: %s.\n"
                                            + "Computed patterns: %s.\n"
                                            + "Try running again.", continuationStr,
                            countingStr, expected, computed));
                }
    }

    public CountCache getOrCreateCountCache() throws IOException {
        if (countCache == null)
            countCache = new CountCache(paths);
        return countCache;
    }

    public CountCache getOrCreateTestCountCache(Path testingFile,
                                                Set<Pattern> neededPatterns) throws IOException {
        // TODO: detect if test file has tagging
        boolean hasPos = false;

        String hash = HashUtils.generateMd5Hash(testingFile);

        Path testCountDir = paths.getQueriesCacheDir().resolve(hash);
        Path testAbsoluteDir = testCountDir.resolve(Constants.ABSOLUTE_DIR_NAME);
        Path testContinuationDir = testCountDir.resolve(Constants.CONTINUATION_DIR_NAME);
        Path testNGramCountsFile = testCountDir.resolve(Constants.NGRAMTIMES_FILE_NAME);
        Path testLengthDistributionFile = testCountDir.resolve(Constants.LENGTHDISTRIBUTION_FILE_NAME);

        LOGGER.info("TestCountCache '%s' -> '%s'.", testingFile, testCountDir);
        LOGGER.debug("Needed Patterns: %s", neededPatterns);

        Files.createDirectories(testAbsoluteDir);
        Files.createDirectories(testContinuationDir);

        if (!NioUtils.checkFile(testNGramCountsFile, EXISTS))
            Files.copy(paths.getNGramTimesFile(), testNGramCountsFile);

        if (!NioUtils.checkFile(testLengthDistributionFile, EXISTS))
            Files.copy(paths.getLengthDistributionFile(),
                    testLengthDistributionFile);

        for (Pattern pattern : neededPatterns) {
            Path countFile, testCountFile;
            if (pattern.isAbsolute()) {
                countFile = paths.getAbsoluteDir().resolve(pattern.toString());
                testCountFile = testAbsoluteDir.resolve(pattern.toString());
            } else {
                countFile = paths.getContinuationDir().resolve(
                        pattern.toString());
                testCountFile = testContinuationDir.resolve(pattern.toString());
            }
            if (NioUtils.checkFile(testCountFile, EXISTS))
                continue;
            else if (!NioUtils.checkFile(countFile, EXISTS))
                throw new IllegalStateException(
                        String.format(
                                "Don't have corpus counts pattern '%s', needed for TestCounts.",
                                pattern));

            SortedSet<String> neededSequences = new TreeSet<String>(
                    extractSequencesForPattern(testingFile, hasPos, pattern));
            filterAndWriteTestCounts(countFile, testCountFile, neededSequences);
        }

        return new CountCache(new GlmtkPaths(testCountDir));
    }

    private Set<String> extractSequencesForPattern(Path testingFile,
                                                   boolean hasPos,
                                                   Pattern pattern) throws IOException {
        Set<String> result = new HashSet<String>();

        int patternSize = pattern.size();
        try (BufferedReader reader = Files.newBufferedReader(testingFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = StringUtils.splitAtChar(line, ' ').toArray(
                        new String[0]);
                String[] words = new String[split.length];
                String[] poses = new String[split.length];
                StringUtils.extractWordsAndPoses(split, hasPos, words, poses);

                for (int p = 0; p <= split.length - patternSize; ++p)
                    result.add(pattern.apply(words, poses, p));
            }
        }

        return result;
    }

    private void filterAndWriteTestCounts(Path countFile,
                                          Path testCountFile,
                                          SortedSet<String> neededSequences) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(countFile,
                Constants.CHARSET);
                BufferedWriter writer = Files.newBufferedWriter(testCountFile,
                        Constants.CHARSET)) {
            String nextSequence = neededSequences.first();

            String line;
            while ((line = reader.readLine()) != null) {
                int p = line.indexOf('\t');
                String sequence = p == -1 ? line : line.substring(0, p);

                int compare;
                while ((compare = sequence.compareTo(nextSequence)) >= 0) {
                    if (compare == 0) {
                        writer.write(line);
                        writer.write('\n');
                    }

                    neededSequences.remove(nextSequence);
                    nextSequence = neededSequences.first();
                }
            }
        } catch (NoSuchElementException e) {
            // neededSequences.first() fails, because neededSequences is empty,
            // so we are done.
        }
    }

    public Query newQuery(String queryTypeString,
                          Path inputFile,
                          Estimator estimator,
                          ProbMode probMode,
                          CountCache countCache) {
        return new Query(queryTypeString, inputFile, paths.getQueriesDir(),
                estimator, probMode, countCache);
    }
}
