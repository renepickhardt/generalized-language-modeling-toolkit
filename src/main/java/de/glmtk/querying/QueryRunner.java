package de.glmtk.querying;

import static de.glmtk.Config.CONFIG;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.CountCache;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public enum QueryRunner {
    QUERY_RUNNER;

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

    private Progress progress;
    private QueryMode queryMode;
    private Path inputFile;
    private Path outputDir;
    private Path outputFile;
    private Estimator estimator;
    private CountCache countCache;
    private Calculator calculator;
    private QueryStats stats;
    private int curLineNo;
    private Queue<String> linesQueue;
    private String[] resultingLines;
    private int readerMemory;
    private int writerMemory;

    public QueryStats runQueriesOnInputStream(QueryMode queryMode,
                                              InputStream inputStream,
                                              OutputStream outputStream,
                                              Estimator estimator,
                                              ProbMode probMode,
                                              CountCache countCache) throws Exception {
        this.queryMode = queryMode;
        this.estimator = estimator;
        this.countCache = countCache;
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
            while ((line = reader.readLine()) != null) {
                writer.write(processLine(line));
                writer.write('\n');
                writer.flush();
            }
            stats.complete();

            List<String> statsOutputLines = StringUtils.splitAtChar(
                    stats.toString(), '\n');
            for (String statsOutputLine : statsOutputLines)
                writer.write("# " + statsOutputLine + '\n');
        }

        return stats;
    }

    public QueryStats runQueriesOnFile(QueryMode queryMode,
                                       Path inputFile,
                                       Path outputDir,
                                       Estimator estimator,
                                       ProbMode probMode,
                                       CountCache countCache) throws Exception {
        this.queryMode = queryMode;
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.estimator = estimator;
        this.countCache = countCache;
        calculator = Calculator.forQueryMode(queryMode);
        stats = new QueryStats();
        outputFile = resolveOutputFile();
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

    private Path resolveOutputFile() {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        return outputDir.resolve(String.format("%s %s %s %s",
                inputFile.getFileName(), estimator.getName(), queryMode, date));
    }

    private void calculateMemory() {
        readerMemory = CONFIG.getReaderMemory();
        writerMemory = CONFIG.getWriterMemory();

        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
    }

    private void queryFile() throws Exception {
        OUTPUT.setPhase(Phase.QUERYING);

        curLineNo = 0;
        linesQueue = new LinkedList<String>();
        try (BufferedReader reader = Files.newBufferedReader(inputFile,
                Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null)
                linesQueue.add(line);
        }

        resultingLines = new String[linesQueue.size()];

        List<Callable<Object>> threads = new LinkedList<Callable<Object>>();
        for (int i = 0; i != CONFIG.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        progress = new Progress(linesQueue.size());
        ThreadUtils.executeThreads(CONFIG.getNumberOfThreads(), threads);

        stats.complete();
    }

    private void assembleFile() throws IOException {
        OUTPUT.setPhase(Phase.ASSEMBLING);

        Files.createDirectories(outputDir);
        Files.deleteIfExists(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile,
                Constants.CHARSET)) {
            for (String line : resultingLines) {
                writer.write(line);
                writer.write('\n');
            }
            resultingLines = null; // Free memory

            List<String> statsOutputLines = StringUtils.splitAtChar(
                    stats.toString(), '\n');
            for (String statsOutputLine : statsOutputLines) {
                writer.append("# ");
                writer.append(statsOutputLine);
                writer.append('\n');
            }
        }
    }

    private String processLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) == '#')
            return line;

        List<String> sequence = StringUtils.splitAtChar(line, ' ');
        double probability = calculator.probability(sequence);
        if (queryMode.isWithLengthFreq() && probability != 0)
            probability *= countCache.getLengthFrequency(sequence.size());

        synchronized (stats) {
            stats.addProbability(probability);
        }

        return line + '\t' + Double.toString(probability);
    }
}
