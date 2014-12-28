package de.glmtk;

import static de.glmtk.utils.NioUtils.CheckFile.EXISTS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.ConsoleOutputter.Phase;
import de.glmtk.Status.TrainingStatus;
import de.glmtk.counting.AbsoluteCounter;
import de.glmtk.counting.ContinuationCounter;
import de.glmtk.counting.CountCache;
import de.glmtk.counting.Counter;
import de.glmtk.counting.LengthDistribution;
import de.glmtk.counting.Tagger;
import de.glmtk.querying.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.calculator.CondCalculator;
import de.glmtk.querying.calculator.MarkovCalculator;
import de.glmtk.querying.calculator.SentenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.utils.HashUtils;
import de.glmtk.utils.NioUtils;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.StringUtils;

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

    private static final DateFormat TEST_FILE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Config config;

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
        try {
            config = Config.getInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

        if (!Files.exists(workingDir)) {
            Files.createDirectories(workingDir);
        }

        status = new Status(statusFile, corpus);
        status.logStatus();
        // TODO: check file system if status is accurate.
    }

    public void count(boolean needPos, Set<Pattern> neededPatterns)
            throws IOException {
        ConsoleOutputter consoleOutputter = ConsoleOutputter.getInstance();

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
                        new Tagger(config.getLogUpdateInterval(),
                                config.getModel());
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

        consoleOutputter.printCorpusAnalyzationInProcess();

        // Absolute ////////////////////////////////////////////////////////////

        AbsoluteCounter absoluteCounter =
                new AbsoluteCounter(neededAbsolute, config.getNumberOfCores(),
                        config.getConsoleUpdateInterval(),
                        config.getLogUpdateInterval());
        absoluteCounter
                .count(status, trainingFile, absoluteDir, absoluteTmpDir);

        // Continuation ////////////////////////////////////////////////////////

        ContinuationCounter continuationCounter =
                new ContinuationCounter(neededContinuation,
                        config.getNumberOfCores(),
                        config.getConsoleUpdateInterval(),
                        config.getLogUpdateInterval());
        continuationCounter.count(status, absoluteDir, absoluteTmpDir,
                continuationDir, continuationTmpDir);

        // Evaluating //////////////////////////////////////////////////////////

        consoleOutputter.setPhase(Phase.EVALUATING, 0.0);

        // N-Gram Times Counts
        LOGGER.info("nGramTimes counting -> '%s'.", nGramTimesFile);
        try (BufferedWriter writer =
                Files.newBufferedWriter(nGramTimesFile,
                        Charset.defaultCharset());
                DirectoryStream<Path> absoluteFiles =
                        Files.newDirectoryStream(absoluteDir)) {
            for (Path absoluteFile : absoluteFiles) {
                long[] nGramTimes = {
                    0L, 0L, 0L, 0L
                };

                try (BufferedReader reader =
                        Files.newBufferedReader(absoluteFile,
                                Charset.defaultCharset())) {
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
        consoleOutputter.setPercent(0.5);

        // Sentence Length Distribution
        if (!NioUtils.checkFile(lengthDistributionFile, EXISTS)) {
            LengthDistribution lengthDistribution =
                    new LengthDistribution(trainingFile, true);
            lengthDistribution.writeToStore(lengthDistributionFile);
        }
        consoleOutputter.setPercent(1.0);

        consoleOutputter.printCorpusAnalyzationDone(NioUtils
                .calcFileSize(Arrays.asList(absoluteDir, continuationDir,
                        nGramTimesFile, lengthDistributionFile)));
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
                        "Don't have corpus counts pattern '" + pattern
                                + "', needed for TestCounts.");
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
                Files.newBufferedReader(testingFile, Charset.defaultCharset())) {
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
                Files.newBufferedReader(countFile, Charset.defaultCharset());
                BufferedWriter writer =
                        Files.newBufferedWriter(testCountFile,
                                Charset.defaultCharset())) {
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

    public void testSentenceFile(
            Path testFile,
            Estimator estimator,
            ProbMode probMode,
            CountCache countCache) throws IOException {
        SentenceCalculator calculator = new SentenceCalculator();
        testFile(testFile, "Sentence", calculator, estimator, probMode,
                countCache, 0, true);
    }

    public void testMarkovFile(
            Path testFile,
            Estimator estimator,
            ProbMode probMode,
            CountCache countCache,
            int order) throws IOException {
        MarkovCalculator calculator = new MarkovCalculator();
        calculator.setOrder(order);
        testFile(testFile, "Markov-" + order, calculator, estimator, probMode,
                countCache, 0, true);
    }

    public void testCondFile(
            Path testFile,
            Estimator estimator,
            ProbMode probMode,
            CountCache countCache,
            int order) throws IOException {
        CondCalculator calculator = new CondCalculator();
        testFile(testFile, "Cond-" + order, calculator, estimator, probMode,
                countCache, order, false);
    }

    private void testFile(
            Path testFile,
            String testType,
            Calculator calculator,
            Estimator estimator,
            ProbMode probMode,
            CountCache countCache,
            int checkOrder,
            boolean multWithLengthFreq) throws IOException {
        ConsoleOutputter consoleOutputter = ConsoleOutputter.getInstance();

        String estimatorName = Estimators.getName(estimator);
        Path outputFile =
                testDir.resolve(TEST_FILE_DATE_FORMAT.format(new Date()) + " "
                        + testType + " " + testFile.getFileName()
                        + (estimatorName == null ? "" : (" " + estimatorName)));
        Files.createDirectories(testDir);
        Files.deleteIfExists(outputFile);

        LOGGER.info("Testing %s File '%s' -> '%s'.", testType, testFile,
                outputFile);

        String msg =
                "Testing " + testType + " File '"
                        + consoleOutputter.bold(testFile.toString()) + "'";
        if (estimatorName != null) {
            msg += " with " + consoleOutputter.bold(estimatorName);
        }
        msg += "...";
        consoleOutputter.printMessage(msg);

        estimator.setCountCache(countCache);
        calculator.setProbMode(probMode);
        calculator.setEstimator(estimator);

        try (BufferedReader reader =
                Files.newBufferedReader(testFile, Charset.defaultCharset());
                BufferedWriter writer =
                        Files.newBufferedWriter(outputFile,
                                Charset.defaultCharset())) {
            TestStats testStats = new TestStats();

            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                line = line.trim();
                if (line.charAt(0) == '#') {
                    writer.append(line);
                    writer.append('\n');
                    continue;
                }

                List<String> sequence = StringUtils.splitAtChar(line, ' ');
                int sequenceSize = sequence.size();
                if (checkOrder != 0 && sequenceSize != checkOrder) {
                    LOGGER.error(
                            "Expected file '%s' to only contains lines of length %s. But line %s has length %s: '%s'.",
                            testFile, checkOrder, lineNo, sequenceSize, line);
                    throw new Termination();
                }
                double probability = calculator.probability(sequence);
                if (multWithLengthFreq && probability != 0) {
                    probability *=
                            countCache.getLengthDistribution()
                                    .getLengthFrequency(sequenceSize);
                }
                testStats.addProbability(probability);

                writer.append(line);
                writer.append('\t');
                writer.append(Double.toString(probability));
                writer.append('\n');
            }

            List<String> testStatsOutput =
                    StringUtils.splitAtChar(testStats.toString(), '\n');
            for (String testStatsOutputLine : testStatsOutput) {
                writer.append("# ");
                writer.append(testStatsOutputLine);
                writer.append('\n');
                LOGGER.info(testStatsOutputLine);
                consoleOutputter.printMessage("    " + testStatsOutputLine);
            }
        }
    }
}
