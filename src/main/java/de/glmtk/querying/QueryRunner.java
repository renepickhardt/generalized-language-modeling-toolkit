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

package de.glmtk.querying;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.Config;
import de.glmtk.common.CountCache;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public class QueryRunner {
    public static final Logger LOGGER = LogManager.getFormatterLogger(QueryRunner.class);

    private class Thread implements Callable<Object> {
        @Override
        public Object call() throws Exception {
            while (true) {
                String line;
                int lineNo;
                synchronized (linesQueue) {
                    line = linesQueue.poll();
                    lineNo = curLineNo;
                    ++curLineNo;
                }
                if (line == null)
                    break;

                resultingLines[lineNo] = processLine(line);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }
    }

    private Config config;

    private Progress progress;
    private QueryMode queryMode;
    private Path inputFile;
    private Path outputFile;
    private CountCache countCache;
    private int corpusOrder;
    private Calculator calculator;
    private QueryStats stats;
    private int curLineNo;
    private Queue<String> linesQueue;
    private String[] resultingLines;
    private int readerMemory;
    private int writerMemory;

    public QueryRunner(Config config) {
        this.config = config;
    }

    public QueryStats runQueriesOnInputStream(QueryMode queryMode,
                                              InputStream inputStream,
                                              OutputStream outputStream,
                                              Estimator estimator,
                                              ProbMode probMode,
                                              CountCache countCache,
                                              int corpusOrder) throws Exception {
        this.queryMode = queryMode;
        this.countCache = countCache;
        this.corpusOrder = corpusOrder;
        calculator = Calculator.forQueryMode(queryMode);
        stats = new QueryStats();
        calculateMemory();

        estimator.setCountCache(countCache);
        calculator.setProbMode(probMode);
        calculator.setEstimator(estimator);

        OUTPUT.printMessage(String.format(
                "Interactive querying with %s estimator...",
                estimator.getName()));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream, Constants.CHARSET));
                OutputStreamWriter writer = new OutputStreamWriter(
                        outputStream, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null)
                writer.append(processLine(line)).append('\n').flush();
            stats.complete();

            List<String> statsOutputLines = StringUtils.splitAtChar(
                    stats.toString(), '\n');
            for (String statsOutputLine : statsOutputLines)
                writer.append("# ").append(statsOutputLine).append('\n');
        }

        return stats;
    }

    public QueryStats runQueriesOnFile(QueryMode queryMode,
                                       Path inputFile,
                                       Path outputFile,
                                       Estimator estimator,
                                       ProbMode probMode,
                                       CountCache countCache,
                                       int corpusOrder) throws Exception {
        this.queryMode = queryMode;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.countCache = countCache;
        this.corpusOrder = corpusOrder;
        calculator = Calculator.forQueryMode(queryMode);
        stats = new QueryStats();
        calculateMemory();

        estimator.setCountCache(countCache);
        calculator.setProbMode(probMode);
        calculator.setEstimator(estimator);

        String message = String.format(
                "Querying %s File '%s' with %s estimation", queryMode,
                OUTPUT.bold(inputFile.toString()), estimator.getName());
        OUTPUT.beginPhases(message + "...");

        queryFile();
        assembleFile();

        OUTPUT.endPhases(message + " done:");

        OUTPUT.printMessage(String.format("    Saved as '%s' under '%s'.",
                OUTPUT.bold(outputFile.getFileName()), outputFile.getParent()));

        List<String> statsOutputLines = StringUtils.splitAtChar(
                stats.toString(), '\n');
        for (String statsOutputLine : statsOutputLines)
            OUTPUT.printMessage("    " + statsOutputLine);

        return stats;
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        writerMemory = config.getMemoryWriter();

        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }

    private void queryFile() throws Exception {
        OUTPUT.setPhase(Phase.QUERYING);

        curLineNo = 0;
        linesQueue = new LinkedList<>();
        try (BufferedReader reader = Files.newBufferedReader(inputFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null)
                linesQueue.add(line);
        }

        resultingLines = new String[linesQueue.size()];

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        progress = OUTPUT.newProgress(linesQueue.size());
        ThreadUtils.executeThreads(config.getNumberOfThreads(), threads);

        stats.complete();
    }

    private void assembleFile() throws IOException {
        OUTPUT.setPhase(Phase.ASSEMBLING);

        Files.deleteIfExists(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile,
                Constants.CHARSET)) {
            for (String line : resultingLines)
                writer.append(line).append('\n');
            resultingLines = null; // Free memory

            List<String> statsOutputLines = StringUtils.splitAtChar(
                    stats.toString(), '\n');
            for (String statsOutputLine : statsOutputLines)
                writer.append("# ").append(statsOutputLine).append('\n');
        }
    }

    private String processLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) == '#')
            return line;

        List<String> sequence = StringUtils.splitAtChar(line, ' ');
        int sequenceOrder = sequence.size();
        Integer modeOrder = queryMode.getOrder();
        if (modeOrder != null && sequenceOrder != modeOrder) {
            try (Formatter f = new Formatter()) {
                f.format(
                        "Illegal sequence. Can only query sequences with length %d when using mode '%s'.",
                        queryMode.getOrder(), modeOrder);
                if (linesQueue != null)
                    // in file mode
                    f.format(" Line %d: '%s'.", curLineNo, line);
                OUTPUT.printWarning(f.toString());
            }
            return line;
        } else if (sequenceOrder > corpusOrder) {
            try (Formatter f = new Formatter()) {
                f.format("Illegal sequence. Corpus size was set to '%d'.",
                        corpusOrder);
                if (linesQueue != null)
                    // in file mode
                    f.format(" Line %d: '%s'.", curLineNo, line);
                OUTPUT.printWarning(f.toString());
            }
            return line;
        }

        double probability = calculator.probability(sequence);
        if (queryMode.isWithLengthFreq() && probability != 0)
            probability *= countCache.getLengthFrequency(sequence.size());

        synchronized (stats) {
            stats.addProbability(probability);
        }

        return line + '\t' + Double.toString(probability);
    }
}
