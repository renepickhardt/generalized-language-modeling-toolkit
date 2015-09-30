package de.glmtk.querying.probability;

import static de.glmtk.output.Output.bold;
import static de.glmtk.output.Output.println;

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
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.StringUtils;


// TODO: optimize for test files that do not completely fit into memory, by quering chunks at a time.
public class FileQueryExecutor extends AbstractWorkerExecutor<String> {
    private static final Logger LOGGER = Logger.get(FileQueryExecutor.class);
    private static final String PHASE_QUERYING = "Querying";
    private static final String PHASE_ASSEMBLING = "Assembling";

    private class Worker extends AbstractWorkerExecutor<String>.Worker {
        @Override
        protected void work(String line,
                            int lineNo) throws Exception {
            resultingLines[lineNo] = executor.queryLine(line, lineNo);
        }
    }

    private QueryExecutor executor;
    private String[] resultingLines;
    private ProgressBar progressBar;

    public FileQueryExecutor(Config config) {
        super(config);
    }

    public QueryStats queryFile(GlmtkPaths paths,
                                QueryMode mode,
                                Estimator estimator,
                                int corpusOrder,
                                Path inputFile,
                                Path outputFile) throws Exception {
        println("Querying '%s' using mode %s with %s estimation...",
            bold(inputFile), mode, estimator);
        LOGGER.info("Querying '%s' using mode %s with %s estimation...",
            inputFile, mode, estimator);

        progressBar = new ProgressBar(PHASE_QUERYING, PHASE_ASSEMBLING);

        executor = new QueryExecutor(paths, mode, estimator, corpusOrder);

        queryLines(inputFile);
        QueryStats stats = assembleFile(outputFile);

        println("    Saved as '%s' under '%s'.", bold(outputFile.getFileName()),
            outputFile.getParent());

        List<String> statsLines = StringUtils.split(stats.toString(), '\n');
        for (String statsLine : statsLines) {
            println("    " + statsLine);
        }

        return stats;
    }

    private void queryLines(Path inputFile) throws Exception {
        progressBar.setPhase(PHASE_QUERYING);

        List<String> lines = Files.readAllLines(inputFile, Constants.CHARSET);
        resultingLines = new String[lines.size()];

        work(lines, progressBar);
    }

    private QueryStats assembleFile(Path outputFile) throws IOException {
        progressBar.setPhase(PHASE_ASSEMBLING);

        Files.deleteIfExists(outputFile);

        QueryStats stats = null;
        try (BufferedWriter writer =
            Files.newBufferedWriter(outputFile, Constants.CHARSET)) {
            for (String line : resultingLines) {
                writer.append(line).append('\n');
            }

            stats = executor.getResultingStats();
            List<String> statsLines = StringUtils.split(stats.toString(), '\n');
            for (String statsLine : statsLines) {
                writer.append("# ").append(statsLine).append('\n');
            }
        }

        resultingLines = null; // Enable gc on memory

        progressBar.set(1.0);

        return stats;
    }

    @Override
    protected Collection<? extends Worker> createWorkers() {
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i) {
            workers.add(new Worker());
        }
        return workers;
    }
}
