package de.glmtk.querying.probability;

import static de.glmtk.common.Output.OUTPUT;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.AbstractWorkerExecutor;
import de.glmtk.common.Config;
import de.glmtk.common.Output.Phase;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.StringUtils;

// TODO: optimize for test files that do not completely fit into memory, by quering chunks at a time.
public class FileQueryExecutor extends AbstractWorkerExecutor<String> {
    private class Worker extends AbstractWorkerExecutor<String>.Worker {
        @Override
        protected void work(String line,
                            int lineNo) throws Exception {
            resultingLines[lineNo] = executor.queryLine(line, lineNo);
        }
    }

    private QueryExecutor executor;
    private String[] resultingLines;

    public FileQueryExecutor(Config config) {
        super(config);
    }

    public QueryStats queryFile(GlmtkPaths paths,
                                QueryMode mode,
                                Estimator estimator,
                                int corpusOrder,
                                Path inputFile,
                                Path outputFile) throws Exception {
        String message = String.format(
                "Querying '%s' using mode %s with %s estimation",
                OUTPUT.bold(inputFile), mode, estimator);
        OUTPUT.beginPhases(message + "...");

        executor = new QueryExecutor(paths, mode, estimator, corpusOrder);

        queryLines(inputFile);
        QueryStats stats = assembleFile(outputFile);

        OUTPUT.endPhases(message + ".");

        OUTPUT.printMessage(String.format("    Saves as '%s' under '%s'.",
                OUTPUT.bold(outputFile.getFileName()), outputFile.getParent()));

        List<String> statsLines = StringUtils.split(stats.toString(),
                '\n');
        for (String statsLine : statsLines)
            OUTPUT.printMessage("    " + statsLine);

        return stats;
    }

    private void queryLines(Path inputFile) throws Exception {
        OUTPUT.setPhase(Phase.QUERYING);

        List<String> lines = Files.readAllLines(inputFile, Constants.CHARSET);
        resultingLines = new String[lines.size()];

        work(lines);
    }

    private QueryStats assembleFile(Path outputFile) throws IOException {
        OUTPUT.setPhase(Phase.ASSEMBLING);

        Files.deleteIfExists(outputFile);

        QueryStats stats = null;
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile,
                Constants.CHARSET)) {
            for (String line : resultingLines)
                writer.append(line).append('\n');

            stats = executor.getResultingStats();
            List<String> statsLines = StringUtils.split(stats.toString(),
                    '\n');
            for (String statsLine : statsLines)
                writer.append("# ").append(statsLine).append('\n');
        }

        resultingLines = null; // Enable gc on memory

        return stats;
    }

    @Override
    protected Collection<? extends Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            workers.add(new Worker());
        return workers;
    }
}
