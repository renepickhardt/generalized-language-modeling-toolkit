/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen
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

package de.glmtk.executables;

import static de.glmtk.logging.Log4jHelper.addLoggingFileAppender;
import static de.glmtk.output.Output.println;
import static de.glmtk.output.Output.printlnError;
import static de.glmtk.util.CollectionUtils.asSortedList;
import static de.glmtk.util.Files.newLineNumberReader;
import static de.glmtk.util.NioUtils.calcFileSize;
import static de.glmtk.util.NioUtils.countNumberOfLines;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;
import static de.glmtk.util.StringUtils.split;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Collections.shuffle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.exceptions.Termination;
import de.glmtk.logging.Logger;
import de.glmtk.options.DoubleOption;
import de.glmtk.options.IntegerOption;
import de.glmtk.options.custom.CorpusOption;
import de.glmtk.output.ProgressBar;
import de.glmtk.util.StringUtils;

public class GlmtkExpSetupExecutable extends Executable {
    private static final Logger LOGGER = Logger.get(
            GlmtkExpSetupExecutable.class);
    private static final String PHASE_SPLITTING_CORPUS = "Splitting Training / Heldout";
    private static final String PHASE_BISECTING_TRAINING = "Bisecting Training";
    private static final String PHASE_BUILDING_VOCABULARY = "Building Vocabulary";
    private static final String PHASE_FILTER_UNK = "Filter Heldout for UNK";
    private static final String PHASE_SELECTING_NGRAMS = "Selecting NGrams";

    public static void main(String[] args) {
        new GlmtkExpSetupExecutable().run(args);
    }

    private CorpusOption optionCorpus;
    private DoubleOption optionTrainingProb;
    private IntegerOption optionNumBisectionSteps;
    private IntegerOption optionNGramLength;
    private IntegerOption optionNumNGrams;
    private IntegerOption optionNumUnkNGrams;

    private Path corpus;
    private Path workingDir;
    private Double trainingProb;
    private int numBisectionSteps;
    private int ngramLength;
    private int numNGrams;
    private int numUnkNGrams;
    private ProgressBar progressBar;

    @Override
    protected String getExecutableName() {
        return "glmtk-exp-setup";
    }

    @Override
    protected void registerOptions() {
        optionCorpus = new CorpusOption(null, "corpus",
                "Give corpus and maybe working directory.").suffix(".expsetup");
        optionTrainingProb = new DoubleOption("p", "training-prob",
                "Probability with which lines go into training, "
                        + "other lines go into held-out. Default: 0.8.").defaultValue(
                                0.8).requireProbability();
        optionNumBisectionSteps = new IntegerOption("b", "num-bisection-steps",
                "Number of times the training file "
                        + "should be cut in half. Default: 5").defaultValue(
                                5).requirePositive().requireNotZero();
        optionNGramLength = new IntegerOption("n", "ngram-length",
                "Lenghts for which n-grams should be selected. "
                        + "If zero no n-grams will be selected. Default: 10.").defaultValue(
                                10).requirePositive().requireNotZero();
        optionNumNGrams = new IntegerOption("N", "num-ngrams",
                "Number of n-gram sequences to select. Default: 10,000").defaultValue(
                        10000).requirePositive().requireNotZero();
        optionNumUnkNGrams = new IntegerOption("U", "num-unk-ngrams",
                "Number of n-gram sequences containing atleast one unkown word to select. Default: same as number of num-ngrams.").requirePositive().requireNotZero();

        commandLine.inputArgs(optionCorpus);
        commandLine.options(optionTrainingProb, optionNumBisectionSteps,
                optionNGramLength, optionNumNGrams, optionNumUnkNGrams);
    }

    @Override
    protected String getHelpHeader() {
        return "Splits the given corpus into training and test files.";
    }

    @Override
    protected String getHelpFooter() {
        return null;
    }

    @Override
    protected void parseOptions(String[] args) throws Exception {
        super.parseOptions(args);

        if (!optionCorpus.wasGiven())
            throw new CliArgumentException("%s missing.", optionCorpus);
        corpus = optionCorpus.getCorpus();
        workingDir = optionCorpus.getWorkingDir();

        trainingProb = optionTrainingProb.getDouble();
        numBisectionSteps = optionNumBisectionSteps.getInt();
        ngramLength = optionNGramLength.getInt();
        numNGrams = optionNumNGrams.getInt();
        if (optionNumUnkNGrams.wasGiven())
            numUnkNGrams = optionNumUnkNGrams.getInt();
        else
            numUnkNGrams = numNGrams;
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();

        addLoggingFileAppender(workingDir.resolve(
                Constants.LOCAL_LOG_FILE_NAME), "FileLocal", true);
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        progressBar = new ProgressBar(PHASE_SPLITTING_CORPUS,
                PHASE_BISECTING_TRAINING, PHASE_BUILDING_VOCABULARY,
                PHASE_FILTER_UNK, PHASE_SELECTING_NGRAMS);

        Path corpusFile = workingDir.resolve("corpus");
        Path trainingFile = workingDir.resolve("training");
        Path heldoutFile = workingDir.resolve("heldout");
        Path minVocabularyFile = workingDir.resolve("vocabulary.min");
        Path maxVocabularyFile = workingDir.resolve("vocabulary.max");
        Path heldoutNoUnkFile = Paths.get(heldoutFile + ".nounk");
        Path heldoutUnkFile = Paths.get(heldoutFile + ".unk");

        println("Experimental setup");

        copy(corpus, corpusFile);
        splitTrainingHeldout(corpusFile, trainingFile, heldoutFile);
        List<Path> bisectionFiles = bisectTraining(trainingFile);
        Set<String> minVocabulary = buildVocabulary(bisectionFiles.get(
                bisectionFiles.size() - 1), minVocabularyFile);
        Set<String> maxVocabulary = buildVocabulary(bisectionFiles.get(0),
                maxVocabularyFile);
        filterUnk(heldoutFile, heldoutNoUnkFile, heldoutUnkFile, minVocabulary,
                maxVocabulary);
        selectNGrams(heldoutNoUnkFile);
        selectUnkNGrams(heldoutUnkFile, minVocabulary, maxVocabulary);

        long workingDirFileSize = calcFileSize(workingDir);
        println("    (took %s)", humanReadableByteCount(workingDirFileSize));
    }

    private void splitTrainingHeldout(Path corpusFile,
                                      Path trainingFile,
                                      Path heldoutFile) throws IOException {
        progressBar.setPhase(PHASE_SPLITTING_CORPUS, countNumberOfLines(
                corpusFile));

        Random rand = new Random();

        try (BufferedReader reader = newBufferedReader(corpusFile,
                Constants.CHARSET);
                BufferedWriter trainingWriter = newBufferedWriter(trainingFile,
                        Constants.CHARSET);
                BufferedWriter heldoutWriter = newBufferedWriter(heldoutFile,
                        Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (rand.nextDouble() <= trainingProb)
                    trainingWriter.append(line).append('\n');
                else
                    heldoutWriter.append(line).append('\n');

                progressBar.increase();
            }
        }
    }

    private List<Path> bisectTraining(Path trainingFile) throws IOException {
        progressBar.setPhase(PHASE_BISECTING_TRAINING, countNumberOfLines(
                trainingFile));

        List<Path> bisectionFiles = new ArrayList<>(numBisectionSteps);
        for (int i = 1; i != numBisectionSteps + 1; ++i)
            bisectionFiles.add(Paths.get(trainingFile + "-" + i));

        List<BufferedWriter> writers = new ArrayList<>(numBisectionSteps);
        for (Path bisectionFile : bisectionFiles)
            writers.add(newBufferedWriter(bisectionFile, Constants.CHARSET));

        try (LineNumberReader reader = newLineNumberReader(trainingFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int lineNumber = reader.getLineNumber();

                int i = 1;
                for (BufferedWriter writer : writers) {
                    if (lineNumber % i == 0)
                        writer.append(line).append('\n');
                    i *= 2;
                }

                progressBar.increase(1);
            }
        }

        for (BufferedWriter writer : writers)
            writer.close();

        return bisectionFiles;
    }

    private Set<String> buildVocabulary(Path trainingFile,
                                        Path vocabularyFile) throws IOException {
        progressBar.setPhase(PHASE_BUILDING_VOCABULARY, countNumberOfLines(
                trainingFile));
        Set<String> vocabulary = new HashSet<>();

        try (BufferedReader reader = newBufferedReader(trainingFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> words = split(line, ' ');
                for (String word : words)
                    vocabulary.add(word);

                progressBar.increase(1);
            }
        }

        List<String> sortedVocabulary = asSortedList(vocabulary);
        try (BufferedWriter writer = newBufferedWriter(vocabularyFile,
                Constants.CHARSET)) {
            for (String word : sortedVocabulary)
                writer.append(word).append('\n');
        }

        return vocabulary;
    }

    private void filterUnk(Path inputFile,
                           Path outputNoUnkFile,
                           Path outputUnkFile,
                           Set<String> minVocabulary,
                           Set<String> maxVocabulary) throws IOException {
        progressBar.setPhase(PHASE_FILTER_UNK, countNumberOfLines(inputFile));

        try (BufferedReader reader = newBufferedReader(inputFile,
                Constants.CHARSET);
                BufferedWriter noUnkWriter = newBufferedWriter(outputNoUnkFile,
                        Constants.CHARSET);
                BufferedWriter unkWriter = newBufferedWriter(outputUnkFile,
                        Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> words = split(line, ' ');
                if (minVocabulary.containsAll(words))
                    noUnkWriter.append(line).append('\n');
                else if (!maxVocabulary.containsAll(words))
                    unkWriter.append(line).append('\n');

                progressBar.increase(1);
            }
        }
    }

    private List<Path> selectNGrams(Path inputFile) throws IOException {
        progressBar.setPhase(PHASE_SELECTING_NGRAMS, numNGrams);

        Random rand = new Random();

        List<List<String>> ngramCandidates = new ArrayList<>();
        try (BufferedReader reader = newBufferedReader(inputFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> words = split(line, ' ');
                if (words.size() >= ngramLength) {
                    int firstWordIndex = rand.nextInt(words.size() - ngramLength
                            + 1);
                    List<String> ngram = words.subList(firstWordIndex,
                            firstWordIndex + ngramLength);
                    ngramCandidates.add(ngram);
                }
            }
        }

        if (ngramCandidates.size() < numNGrams) {
            printlnError("Not enough available NGram "
                    + "sequences that are longer than requested size. "
                    + "Size = %d, Have = %d, Need = %d", ngramLength,
                    ngramCandidates.size(), numNGrams);
            throw new Termination();
        }

        List<Path> ngramFiles = new ArrayList<>(ngramLength);
        for (int i = 1; i != ngramLength + 1; ++i)
            ngramFiles.add(workingDir.resolve("ngram-" + i));

        List<BufferedWriter> writers = new ArrayList<>(ngramLength);
        for (Path ngramFile : ngramFiles)
            writers.add(newBufferedWriter(ngramFile, Constants.CHARSET));

        for (int i = 0; i != numNGrams; ++i) {
            int candidateIndex = rand.nextInt(ngramCandidates.size());
            List<String> ngramWords = ngramCandidates.remove(candidateIndex);

            for (int j = ngramLength - 1; j != -1; --j) {
                writers.get(j).append(StringUtils.join(ngramWords, ' ')).append(
                        '\n');
                ngramWords.remove(0);
            }

            progressBar.increase();
        }

        for (BufferedWriter writer : writers)
            writer.close();

        return ngramFiles;
    }

    private List<Path> selectUnkNGrams(Path inputFile,
                                       Set<String> minVocabulary,
                                       Set<String> maxVocabulary) throws IOException {
        progressBar.setPhase(PHASE_SELECTING_NGRAMS, numUnkNGrams);

        Random rand = new Random();

        List<List<String>> ngramCandidates = new ArrayList<>();
        try (BufferedReader reader = newBufferedReader(inputFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> words = split(line, ' ');
                if (words.size() >= ngramLength) {
                    int maxFirstWordIndex = words.size() - ngramLength + 1;
                    List<Integer> firstWordIndexCanidates = new ArrayList<>(
                            maxFirstWordIndex);
                    for (int i = 0; i != maxFirstWordIndex; ++i)
                        firstWordIndexCanidates.add(i);
                    shuffle(firstWordIndexCanidates, rand);

                    for (int firstWordIndex : firstWordIndexCanidates) {
                        List<String> ngram = words.subList(firstWordIndex,
                                firstWordIndex + ngramLength);
                        List<String> history = ngram.subList(0, ngram.size()
                                - 2);
                        String wordToPredict = ngram.get(ngram.size() - 1);
                        if (!maxVocabulary.containsAll(history)
                                && minVocabulary.contains(wordToPredict)) {
                            ngramCandidates.add(ngram);
                            break;
                        }
                    }
                }
            }
        }

        if (ngramCandidates.size() < numUnkNGrams) {
            printlnError("Not enough available NGram sequences "
                    + "containing unk that are longer than requested size. "
                    + "Size = %d, Have = %d, Need = %d", ngramLength,
                    ngramCandidates.size(), numUnkNGrams);
            throw new Termination();
        }

        List<Path> ngramFiles = new ArrayList<>(ngramLength);
        for (int i = 1; i != ngramLength + 1; ++i)
            ngramFiles.add(workingDir.resolve("ngram.unk-" + i));

        List<BufferedWriter> writers = new ArrayList<>(ngramLength);
        for (Path ngramFile : ngramFiles)
            writers.add(newBufferedWriter(ngramFile, Constants.CHARSET));

        for (int i = 0; i != numUnkNGrams; ++i) {
            int candidateIndex = rand.nextInt(ngramCandidates.size());
            List<String> ngramWords = ngramCandidates.remove(candidateIndex);

            for (int j = ngramLength - 1; j != -1; --j) {
                writers.get(j).append(StringUtils.join(ngramWords, ' ')).append(
                        '\n');
                ngramWords.remove(0);
            }

            progressBar.increase();
        }

        for (BufferedWriter writer : writers)
            writer.close();

        return ngramFiles;
    }

    private void logFields() {
        LOGGER.debug("%s %s", getExecutableName(), StringUtils.repeat("-", 80
                - getExecutableName().length()));
        LOGGER.debug("Corpus:            %s", corpus);
        LOGGER.debug("WorkingDir:        %s", workingDir);
        LOGGER.debug("TrainingProb:      %f", trainingProb);
        LOGGER.debug("NumBisectionSteps: %d", numBisectionSteps);
        LOGGER.debug("NGramLength:       %d", ngramLength);
        LOGGER.debug("NumNGrams:         %d", numNGrams);
        LOGGER.debug("NumUnkNGrams:      %d", numUnkNGrams);
    }
}
