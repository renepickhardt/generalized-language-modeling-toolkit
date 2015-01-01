package de.glmtk;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.counting.AbsoluteChunker.ABSOLUTE_CHUNKER;
import static de.glmtk.counting.ContinuationChunker.CONTINUATION_CHUNKER;
import static de.glmtk.counting.Merger.MERGER;
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

import de.glmtk.Status.TrainingStatus;
import de.glmtk.common.CountCache;
import de.glmtk.common.Counter;
import de.glmtk.common.Output;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.ProbMode;
import de.glmtk.counting.LengthDistribution;
import de.glmtk.counting.Tagger;
import de.glmtk.querying.Query;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.HashUtils;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;

/**
 * Here happens counting (during training phase) and also querying (during
 * application phase) This class is been called either by one of the Executable
 * classes and filled from config or console input or it is called from
 * UnitTests
 * Expects parameters to be set via setters before calling any other method
 * TODO: what about default parameters
 *
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

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Glmtk.class);

    private Path corpus;

    private Path workingDir;

    private Path statusFile;

    private Path trainingFile;

    private Path absoluteDir;

    private Path absoluteTmpDir;

    private Path continuationDir;

    private Path continuationTmpDir;

    private Path nGramTimesFile;

    private Path lengthDistributionFile;

    private Path testCountsDir;

    private Path testDir;

    private Status status;

    private CountCache countCache = null;

    public Glmtk(
            Path corpus,
            Path workingDir) throws IOException {
        this.corpus = corpus;
        this.workingDir = workingDir;
        statusFile = workingDir.resolve(Constants.STATUS_FILE_NAME);
        trainingFile = workingDir.resolve(Constants.TRAINING_FILE_NAME);
        absoluteDir = workingDir.resolve(Constants.ABSOLUTE_DIR_NAME);
        absoluteTmpDir =
                workingDir.resolve(Constants.ABSOLUTE_DIR_NAME + ".tmp");
        continuationDir = workingDir.resolve(Constants.CONTINUATION_DIR_NAME);
        continuationTmpDir =
                workingDir.resolve(Constants.CONTINUATION_DIR_NAME + ".tmp");
        nGramTimesFile = workingDir.resolve(Constants.NGRAMTIMES_FILE_NAME);
        lengthDistributionFile =
                workingDir.resolve(Constants.LENGTHDISTRIBUTION_FILE_NAME);
        testCountsDir = workingDir.resolve("testcounts");
        testDir = workingDir.resolve("testing");

        Files.createDirectories(workingDir);

        status = new Status(statusFile, corpus);
        status.logStatus();
        // TODO: check file system if status is accurate.
    }

    public void count(boolean needPos, Set<Pattern> neededPatterns)
            throws IOException, InterruptedException {
        // TODO: update status with smaller increments (each completed pattern).

        Set<Pattern> neededAbsolute = new HashSet<Pattern>();
        Set<Pattern> neededContinuation = new HashSet<Pattern>();

        Queue<Pattern> neededPatternsQueue =
                new LinkedList<Pattern>(neededPatterns);
        while (!neededPatternsQueue.isEmpty()) {
            Pattern pattern = neededPatternsQueue.poll();
            if (pattern.isAbsolute()) {
                neededAbsolute.add(pattern);
            } else {
                neededContinuation.add(pattern);
                Pattern source = pattern.getContinuationSource();
                if ((source.isAbsolute() ? neededAbsolute : neededContinuation)
                        .add(source)) {
                    neededPatternsQueue.add(source);
                }
            }
        }

        LOGGER.debug("Counting %s", StringUtils.repeat("-", 80 - 9));
        LOGGER.debug("needPos            = %s", needPos);
        LOGGER.debug("neededAbsolute     = %s", neededAbsolute);
        LOGGER.debug("neededContinuation = %s", neededContinuation);

        // Training / Tagging //////////////////////////////////////////////////

        // TODO: Need to check if training is already tagged and act accordingly.
        // TODO: doesn't detect the setting that user changed from untagged training file, to tagged file with same corpus.
        // TODO: doesn't detect when switching from untagged training to continuing with now tagged corpus.
        if (needPos) {
            if (status.getTraining() == TrainingStatus.DONE_WITH_POS) {
                LOGGER.info("Detected tagged training already present, skipping tagging.");
            } else {
                if (corpus.equals(trainingFile)) {
                    Path tmpCorpus = Files.createTempFile("", "");
                    Files.copy(corpus, tmpCorpus);
                    corpus = tmpCorpus;
                }

                Tagger tagger =
                        new Tagger(CONFIG.getLogUpdateInterval(),
                                CONFIG.getModel());
                Files.deleteIfExists(trainingFile);
                tagger.tag(corpus, trainingFile);
                status.setTraining(TrainingStatus.DONE_WITH_POS, trainingFile);
            }
        } else {
            if (status.getTraining() != TrainingStatus.NONE) {
                LOGGER.info("Detected training already present, skipping copying training.");
            } else {
                if (!corpus.equals(trainingFile)) {
                    Files.deleteIfExists(trainingFile);
                    Files.copy(corpus, trainingFile);
                }
                status.setTraining(TrainingStatus.DONE, trainingFile);
            }
        }

        OUTPUT.beginPhases("Corpus Analyzation...");

        countAbsolute(neededAbsolute);
        countContinuation(neededContinuation);

        // Evaluating //////////////////////////////////////////////////////////

        OUTPUT.setPhase(Phase.EVALUATING, true);
        Progress progress = new Progress(2);

        // N-Gram Times Counts
        LOGGER.info("nGramTimes counting -> '%s'.", nGramTimesFile);
        try (BufferedWriter writer =
                Files.newBufferedWriter(nGramTimesFile, Constants.CHARSET);
                DirectoryStream<Path> absoluteFiles =
                        Files.newDirectoryStream(absoluteDir)) {
            for (Path absoluteFile : absoluteFiles) {
                long[] nGramTimes = {
                        0L, 0L, 0L, 0L
                };

                try (BufferedReader reader =
                        Files.newBufferedReader(absoluteFile, Constants.CHARSET)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Counter counter = new Counter();
                        Counter.getSequenceAndCounter(line, counter);

                        // downcast is ok here
                        int count = (int) counter.getOnePlusCount();

                        if (count == 0 || count > 4) {
                            continue;
                        }
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

        // Sentence Length Distribution
        if (!NioUtils.checkFile(lengthDistributionFile, EXISTS)) {
            LengthDistribution lengthDistribution =
                    new LengthDistribution(trainingFile, true);
            lengthDistribution.writeToStore(lengthDistributionFile);
        }
        progress.increase(1);

        long corpusSize =
                NioUtils.calcFileSize(Arrays
                        .asList(absoluteDir, continuationDir, nGramTimesFile,
                                lengthDistributionFile));
        OUTPUT.endPhases(String.format("Corpus Analyzation done (uses %s).",
                Output.humanReadableByteCount(corpusSize, false)));
    }

    private void countAbsolute(Set<Pattern> neededPatterns) throws IOException,
            InterruptedException {
        LOGGER.info("Absolute counting '%s' -> '%s'.", trainingFile,
                absoluteDir);

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(false));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        countingPatterns.removeAll(status.getChunkedPatterns(false));

        ABSOLUTE_CHUNKER.chunk(status, chunkingPatterns, trainingFile,
                absoluteTmpDir);
        MERGER.merge(status, false, countingPatterns, absoluteTmpDir,
                absoluteDir);
    }

    private void countContinuation(Set<Pattern> neededPatterns)
            throws IOException, InterruptedException {
        LOGGER.info("Continuation counting '%s' -> '%s'.", absoluteDir,
                continuationDir);

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(true));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(neededPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(true));

        CONTINUATION_CHUNKER.chunk(status, chunkingPatterns, absoluteDir,
                absoluteTmpDir, continuationDir, continuationTmpDir);
        MERGER.merge(status, true, countingPatterns, continuationTmpDir,
                continuationDir);
    }

    public CountCache getOrCreateCountCache() throws IOException {
        if (countCache == null) {
            countCache = new CountCache(workingDir);
        }
        return countCache;
    }

    public CountCache getOrCreateTestCountCache(
            Path testingFile,
            Set<Pattern> neededPatterns) throws IOException {
        // TODO: detect if test file has pos
        boolean hasPos = false;

        String hash = HashUtils.generateMd5Hash(testingFile);

        Path testCountDir = testCountsDir.resolve(hash);
        Path testAbsoluteDir =
                testCountDir.resolve(Constants.ABSOLUTE_DIR_NAME);
        Path testContinuationDir =
                testCountDir.resolve(Constants.CONTINUATION_DIR_NAME);
        Path testNGramCountsFile =
                testCountDir.resolve(Constants.NGRAMTIMES_FILE_NAME);
        Path testLengthDistributionFile =
                testCountDir.resolve(Constants.LENGTHDISTRIBUTION_FILE_NAME);

        LOGGER.info("TestCountCache '%s' -> '%s'.", testingFile, testCountDir);
        LOGGER.debug("Needed Patterns: %s", neededPatterns);

        Files.createDirectories(testAbsoluteDir);
        Files.createDirectories(testContinuationDir);

        if (!NioUtils.checkFile(testNGramCountsFile, EXISTS)) {
            Files.copy(nGramTimesFile, testNGramCountsFile);
        }

        if (!NioUtils.checkFile(testLengthDistributionFile, EXISTS)) {
            Files.copy(lengthDistributionFile, testLengthDistributionFile);
        }

        for (Pattern pattern : neededPatterns) {
            Path countFile, testCountFile;
            if (pattern.isAbsolute()) {
                countFile = absoluteDir.resolve(pattern.toString());
                testCountFile = testAbsoluteDir.resolve(pattern.toString());
            } else {
                countFile = continuationDir.resolve(pattern.toString());
                testCountFile = testContinuationDir.resolve(pattern.toString());
            }
            if (NioUtils.checkFile(testCountFile, EXISTS)) {
                continue;
            } else if (!NioUtils.checkFile(countFile, EXISTS)) {
                throw new IllegalStateException(
                        String.format(
                                "Don't have corpus counts pattern '%s', needed for TestCounts.",
                                pattern));
            }

            SortedSet<String> neededSequences =
                    new TreeSet<String>(extractSequencesForPattern(testingFile,
                            hasPos, pattern));
            filterAndWriteTestCounts(countFile, testCountFile, neededSequences);
        }

        return new CountCache(testCountDir);
    }

    private Set<String> extractSequencesForPattern(
            Path testingFile,
            boolean hasPos,
            Pattern pattern) throws IOException {
        Set<String> result = new HashSet<String>();

        int patternSize = pattern.size();
        try (BufferedReader reader =
                Files.newBufferedReader(testingFile, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split =
                        StringUtils.splitAtChar(line, ' ').toArray(
                                new String[0]);
                String[] words = new String[split.length];
                String[] poses = new String[split.length];
                StringUtils.extractWordsAndPoses(split, hasPos, words, poses);

                for (int p = 0; p <= split.length - patternSize; ++p) {
                    result.add(pattern.apply(words, poses, p));
                }
            }
        }

        return result;
    }

    private void filterAndWriteTestCounts(
            Path countFile,
            Path testCountFile,
            SortedSet<String> neededSequences) throws IOException {
        try (BufferedReader reader =
                Files.newBufferedReader(countFile, Constants.CHARSET);
                BufferedWriter writer =
                        Files.newBufferedWriter(testCountFile,
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

    public Query newQuery(
            String queryTypeString,
            Path inputFile,
            Estimator estimator,
            ProbMode probMode,
            CountCache countCache) {
        return new Query(queryTypeString, inputFile, testDir, estimator,
                probMode, countCache);
    }

}
