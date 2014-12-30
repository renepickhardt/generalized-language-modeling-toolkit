package de.glmtk.querying;

import static de.glmtk.common.Console.CONSOLE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.common.CountCache;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.util.StringUtils;

public class Query {

    public static class QueryStats {

        private int cntZero = 0;

        private int cntNonZero = 0;

        private double sum = 0.0;

        private double entropy = 0.0;

        private double crossEntropy = 0.0;

        public void addProbability(double probability) {
            if (probability == 0) {
                ++cntZero;
            } else {
                double logProbability = Math.log(probability);
                ++cntNonZero;
                sum += probability;
                entropy -= probability * logProbability;
                crossEntropy -= logProbability;
            }
        }

        @Override
        public void finalize() {
            if (cntNonZero != 0) {
                double baseLog = Math.log(Constants.LOG_BASE);
                entropy /= baseLog;
                crossEntropy /= (cntNonZero * baseLog);
            }
        }

        @Override
        public String toString() {
            int cnt = cntZero + cntNonZero;
            if (cnt == 0) {
                // Avoid division by zero.
                cnt = 1;
            }

            NumberFormat percentFormatter = NumberFormat.getPercentInstance();
            percentFormatter.setMaximumFractionDigits(2);
            percentFormatter.setMinimumFractionDigits(2);

            try (Formatter f = new Formatter()) {
                f.format("Num Sequences (Prob != 0): %d (%.2f)\n", cntNonZero,
                        100.0 * cntNonZero / cnt);
                f.format("Num Sequences (Prob == 0): %d (%.2f)\n", cntZero,
                        100.0 * cntZero / cnt);
                f.format("Sum Probabilities........: %f\n", sum);
                f.format("Entropy..................: %f %s\n", entropy,
                        getEntropyUnit(Constants.LOG_BASE));
                f.format("Cross-Entropy............: %f %s\n", crossEntropy,
                        getEntropyUnit(Constants.LOG_BASE));
                return f.toString();
            }
        }

        public static String getEntropyUnit(double logBase) {
            if (logBase == 2.0) {
                return "Sh";
            } else if (logBase == 10.0) {
                return "Hart";
            } else if (logBase == Math.E) {
                return "nat";
            } else {
                return "";
            }
        }

    }

    public static final Logger LOGGER = LogManager
            .getFormatterLogger(Query.class);

    private String queryTypeString;

    private QueryType queryType;

    private Path inputFile;

    private Path outputDir;

    private Estimator estimator;

    private String estimatorName;

    private ProbMode probMode;

    private CountCache countCache;

    private Calculator calculator;

    public Query(
            String queryTypeString,
            Path inputFile,
            Path outputDir,
            Estimator estimator,
            ProbMode probMode,
            CountCache countCache) {
        this.queryTypeString = queryTypeString;
        queryType = QueryType.fromString(queryTypeString);
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.estimator = estimator;
        estimatorName = Estimators.getName(estimator);
        this.probMode = probMode;
        this.countCache = countCache;
        calculator = Calculator.forQueryTypeString(queryTypeString);
    }

    public QueryStats run() throws IOException {
        Path outputFile = resolveOutputFile();
        Files.createDirectories(outputDir);
        Files.deleteIfExists(outputFile);

        LOGGER.info("Testing %s File '%s' -> '%s'.", queryTypeString,
                inputFile, outputFile);
        String estimatorMsg =
                estimatorName == null ? "" : " with "
                        + CONSOLE.bold(estimatorName);
        CONSOLE.printMessage(String.format("Testing %s File '%s'%s...",
                queryTypeString, CONSOLE.bold(inputFile.toString()),
                estimatorMsg));

        estimator.setCountCache(countCache);
        calculator.setProbMode(probMode);
        calculator.setEstimator(estimator);

        QueryStats stats;
        try (BufferedWriter writer =
                Files.newBufferedWriter(outputFile, Constants.CHARSET)) {
            stats = queryFile(inputFile, writer);

            List<String> statsOutputLines =
                    StringUtils.splitAtChar(stats.toString(), '\n');
            for (String statsOutputLine : statsOutputLines) {
                writer.append("# ");
                writer.append(statsOutputLine);
                writer.append('\n');
                LOGGER.info(statsOutputLine);
                CONSOLE.printMessage("    " + statsOutputLine);
            }
        }
        return stats;
    }

    private Path resolveOutputFile() {
        String date =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        return outputDir.resolve(String.format("%s %s %s%s", date,
                queryType.toString(), inputFile.getFileName(),
                estimatorName == null ? "" : (" " + estimatorName)));
    }

    private QueryStats queryFile(Path file, BufferedWriter writer)
            throws IOException {
        QueryStats stats = new QueryStats();

        try (BufferedReader reader =
                Files.newBufferedReader(file, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.charAt(0) == '#') {
                    writer.append(line);
                    writer.append('\n');
                    continue;
                }

                List<String> sequence = StringUtils.splitAtChar(line, ' ');
                int sequenceSize = sequence.size();
                double probability = calculator.probability(sequence);
                if ((queryType == QueryType.SEQUENCE || queryType == QueryType.MARKOV)
                        && probability != 0) {
                    probability *=
                            countCache.getLengthDistribution()
                            .getLengthFrequency(sequenceSize);
                }
                stats.addProbability(probability);

                writer.append(line);
                writer.append('\t');
                writer.append(Double.toString(probability));
                writer.append('\n');
            }
            stats.finalize();

            return stats;
        }
    }

}
