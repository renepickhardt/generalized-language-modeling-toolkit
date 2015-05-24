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

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;
import static de.glmtk.util.NioUtils.CheckFile.EXISTS;
import static de.glmtk.util.NioUtils.CheckFile.IS_NO_DIRECTORY;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.glmtk.Constants;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.logging.Logger;
import de.glmtk.options.DoubleOption;
import de.glmtk.options.IntegerOption;
import de.glmtk.options.PathOption;
import de.glmtk.util.NioUtils;
import de.glmtk.util.PrintUtils;
import de.glmtk.util.StringUtils;

public class GlmtkExpSetupExecutable extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkExpSetupExecutable.class);

    public static void main(String[] args) {
        new GlmtkExpSetupExecutable().run(args);
    }

    private PathOption optionWorkingDir;
    private DoubleOption optionTrainingProb;
    private IntegerOption optionNGramLength;
    private IntegerOption optionNumNGrams;

    private Path corpus = null;
    private Path workingDir = null;
    private Double trainingProb = null;
    private Integer ngramLength = null;
    private Integer numNGrams = null;

    @Override
    protected String getExecutableName() {
        return "glmtk-exp-setup";
    }

    @Override
    protected void options() {
        optionWorkingDir = new PathOption("w", "workingdir",
                "Working directory.").mayExist().needDirectory();
        optionTrainingProb = new DoubleOption("p", "training-prob",
                "Probability with which lines go into training, "
                        + "other lines go into held-out. Default: 0.8.").mustBeProbability();
        optionNGramLength = new IntegerOption("n", "ngram-length",
                "Lenghts for which n-grams should be selected. "
                        + "If zero no n-grams will be selected. Default: 10.").mustBePositive().mustNotBeZero();
        optionNumNGrams = new IntegerOption("N", "num-ngrams",
                "Number of n-gram sequences to select. Default: 5000").mustBePositive().mustNotBeZero();

        optionManager.register(optionWorkingDir, optionTrainingProb,
                optionNGramLength, optionNumNGrams);
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

        corpus = parseInputArg();
        //        parseFlags();

        if (workingDir == null)
            workingDir = Paths.get(corpus + Constants.EXPSETUP_DIR_SUFFIX);
        if (NioUtils.checkFile(workingDir, EXISTS, IS_NO_DIRECTORY))
            throw new IOException(
                    String.format(
                            "Working directory '%s' already exists but is not a directory.",
                            workingDir));

        if (trainingProb == null)
            trainingProb = 0.8;

        if (ngramLength == null)
            ngramLength = 10;

        if (numNGrams == null)
            numNGrams = 5000;
    }

    //    private void parseFlags() {
    //        @SuppressWarnings("unchecked")
    //        Iterator<Option> iter = line.iterator();
    //        while (iter.hasNext()) {
    //            Option option = iter.next();
    //
    //            if (option.equals(OPTION_WORKINGDIR)) {
    //                optionFirstTimeOrFail(workingDir, option);
    //                workingDir = Paths.get(option.getValue());
    //
    //            } else if (option.equals(OPTION_TRAINING_PROB)) {
    //                optionFirstTimeOrFail(trainingProb, option);
    //                trainingProb = optionProbabilityOrFail(option.getValue(),
    //                        "Illegal %s argument", makeOptionString(option));
    //
    //            } else if (option.equals(OPTION_NGRAM_LENGTH)) {
    //                optionFirstTimeOrFail(ngramLength, option);
    //                ngramLength = optionPositiveIntOrFail(option.getValue(), true,
    //                        "Illegal %s argument", makeOptionString(option));
    //
    //            } else if (option.equals(OPTION_NUM_NGRAMS)) {
    //                optionFirstTimeOrFail(numNGrams, option);
    //                numNGrams = optionPositiveIntOrFail(option.getValue(), false,
    //                        "Illegal %s argument", makeOptionString(option));
    //
    //            } else
    //                throw new CliArgumentException(String.format(
    //                        "Unexpected option: '%s'.", option));
    //        }
    //    }

    @Override
    protected void configureLogging() {
        super.configureLogging();

        LOGGING_HELPER.addFileAppender(
                workingDir.resolve(Constants.LOCAL_LOG_FILE_NAME), "FileLocal",
                true);
    }

    @Override
    protected void exec() throws Exception {
        logFields();

        Path trainingFile = workingDir.resolve("training");
        Path heldoutFile = workingDir.resolve("heldout");

        Random rand = new Random();

        List<String> ngramCandidates = new ArrayList<>();

        long corpusFileSize = NioUtils.calcFileSize(corpus);

        String message = "Experimental setup";
        OUTPUT.beginPhases(message + "...");
        OUTPUT.setPhase(Phase.SPLITTING_CORPUS);
        Progress progress = OUTPUT.newProgress(NioUtils.calcNumberOfLines(corpus));
        try (BufferedReader reader = Files.newBufferedReader(corpus,
                Constants.CHARSET);
                BufferedWriter trainingWriter = Files.newBufferedWriter(
                        trainingFile, Constants.CHARSET);
                BufferedWriter heldoutWriter = Files.newBufferedWriter(
                        heldoutFile, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (rand.nextDouble() <= trainingProb)
                    trainingWriter.append(line).append('\n');
                else {
                    heldoutWriter.append(line).append('\n');
                    if (ngramLength != 0
                            && StringUtils.split(line, ' ').size() >= ngramLength)
                        ngramCandidates.add(line);
                }
                progress.increase(1);
            }
        }

        if (ngramLength != 0)
            selectNGrams(ngramCandidates);

        OUTPUT.endPhases(String.format("%s done (%s)", message,
                PrintUtils.humanReadableByteCount(corpusFileSize)));
    }

    private void selectNGrams(List<String> ngramCandidates) throws IOException {
        if (ngramCandidates.size() < numNGrams) {
            OUTPUT.printError(String.format(
                    "Not enough available NGram sequences that are longer than requested size. Size = %d, Have = %d, Need = %d",
                    ngramLength, ngramCandidates.size(), numNGrams));
            return;
        }

        OUTPUT.setPhase(Phase.SELECTING_NGRAMS);
        Progress progress = OUTPUT.newProgress(numNGrams);

        Random rand = new Random();

        List<BufferedWriter> ngramWriters = new ArrayList<>(ngramLength);
        for (int i = 1; i != ngramLength + 1; ++i) {
            Path ngramFile = workingDir.resolve("ngram" + i);
            ngramWriters.add(Files.newBufferedWriter(ngramFile,
                    Constants.CHARSET));
        }

        for (int i = 0; i != numNGrams; ++i) {
            int candidateIndex = rand.nextInt(ngramCandidates.size());
            String ngram = ngramCandidates.remove(candidateIndex);
            List<String> ngramWords = StringUtils.split(ngram, ' ');
            int firstWordIndex = rand.nextInt(ngramWords.size() - ngramLength
                    + 1);
            ngramWords = ngramWords.subList(firstWordIndex, firstWordIndex
                    + ngramLength);
            //            for (int j = ngramWords.size(); j != ngramLength; --j)
            //                ngramWords.remove(ngramWords.size() - 1);

            for (int j = ngramLength - 1; j != -1; --j) {
                ngramWriters.get(j).append(StringUtils.join(ngramWords, ' ')).append(
                        '\n');
                ngramWords.remove(ngramWords.size() - 1);
            }

            progress.increase(1);
        }

        for (BufferedWriter writer : ngramWriters)
            writer.close();
    }

    private void logFields() {
        LOGGER.debug("%s %s", getExecutableName(), StringUtils.repeat("-",
                80 - getExecutableName().length()));
        LOGGER.debug("Corpus:            %s", corpus);
        LOGGER.debug("WorkingDir:        %s", workingDir);
        LOGGER.debug("ProbTraining:      %f", trainingProb);
        LOGGER.debug("MaxNGramLength:    %d", ngramLength);
        LOGGER.debug("NumNGramSequences: %d", numNGrams);
    }
}
