package de.glmtk.querying.probability;

import static de.glmtk.output.Output.println;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Config;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.StringUtils;

public class StreamQueryExecutor {
    @SuppressWarnings("unused")
    private Config config;

    public StreamQueryExecutor(Config config) {
        this.config = config;
    }

    public QueryStats queryStream(GlmtkPaths paths,
                                  QueryMode mode,
                                  Estimator estimator,
                                  int corpusOrder,
                                  InputStream inputStream,
                                  OutputStream outputStream) throws IOException {
        QueryExecutor executor = new QueryExecutor(paths, mode, estimator,
                corpusOrder);

        println("Interactive querying with %s estimator...",
                estimator.getName());

        QueryStats stats = null;
        try (LineNumberReader reader = new LineNumberReader(new BufferedReader(
                new InputStreamReader(inputStream, Constants.CHARSET)));
                OutputStreamWriter writer = new OutputStreamWriter(
                        outputStream, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String l = executor.queryLine(line, reader.getLineNumber());
                writer.append(l).append('\n').flush();
            }

            stats = executor.getResultingStats();
            List<String> statsLines = StringUtils.split(stats.toString(), '\n');
            for (String statsLine : statsLines)
                writer.append("# ").append(statsLine).append('\n');
        }

        return stats;
    }
}
