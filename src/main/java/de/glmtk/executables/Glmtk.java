package de.glmtk.executables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.smoothing.Corpus;
import de.glmtk.smoothing.legacy2.BackoffEstimator;
import de.glmtk.smoothing.legacy2.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.smoothing.legacy2.DeleteCalculator;
import de.glmtk.smoothing.legacy2.Estimator;
import de.glmtk.smoothing.legacy2.FalseMaximumLikelihoodEstimator;
import de.glmtk.smoothing.legacy2.MaximumLikelihoodEstimator;
import de.glmtk.smoothing.legacy2.PropabilityCalculator;
import de.glmtk.smoothing.legacy2.SkipCalculator;
import de.glmtk.utils.StringUtils;

public class Glmtk extends Executable {

    private static final int LOG_BASE = 10;

    private static final List<String> SMOOTHERS = Arrays.asList("mle", "dmle",
            "fmle", "cmle", "dcmle", "bmle", "dbmle");

    private static final String OPTION_SMOOTHER = "smoother";

    private static final String OPTION_TESTING = "testing";

    private static final String OPTION_CROSSPRODUCT = "cross-product";

    private static Logger logger = LogManager.getFormatterLogger(Glmtk.class);

    private static List<Option> options;
    static {
        //@formatter:off
        Option help         = new Option("h", OPTION_HELP,         false, "Print this message.");
        Option version      = new Option("v", OPTION_VERSION,      false, "Print the version information and exit.");
        Option smoother     = new Option("s", OPTION_SMOOTHER,     true,  StringUtils.join(SMOOTHERS, ", ") + ".");
        smoother.setArgName("SMOOTHER");
        Option testing      = new Option("t", OPTION_TESTING,      true,  "Testing sequences files. (exclusive with -c).");
        testing.setArgName("TESTING");
        Option crossproduct = new Option("c", OPTION_CROSSPRODUCT, true,  "Use cross product of words up to N. (exclusive with -t).");
        crossproduct.setArgName("N");
        //@formatter:on
        options = Arrays.asList(help, version, smoother, testing, crossproduct);
    }

    private Path corpusDir = null;

    private String smoother = "mle";

    private Path testing = null;

    private Integer crossProductSize = null;

    private Corpus corpus = null;

    private PropabilityCalculator calculator = null;

    public static void main(String[] args) {
        new Glmtk().run(args);
    }

    @Override
    protected List<Option> getOptions() {
        return options;
    }

    @Override
    protected String getUsage() {
        return "glmtk [OPTION]... [CORPUS]";
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);

        if (line.getArgs() == null || line.getArgs().length == 0) {
            corpusDir = Paths.get(".");
        } else {
            corpusDir = Paths.get(line.getArgs()[0]);
        }
        if (!Files.exists(corpusDir)) {
            System.err.println("Corpus \"" + corpusDir + "\" does not exist.");
            throw new Termination();
        }
        if (!Files.isDirectory(corpusDir)) {
            System.err.println("Corpus \"" + corpusDir
                    + "\" is not a directory.");
            throw new Termination();
        }
        if (!Files.isReadable(corpusDir)) {
            System.err.println("Corpus \"" + corpusDir + "\" is not readable.");
            throw new Termination();
        }

        if (line.hasOption(OPTION_SMOOTHER)) {
            smoother = line.getOptionValue(OPTION_SMOOTHER);
        }
        if (!SMOOTHERS.contains(smoother)) {
            System.err.println("Invalid Smoother \"" + smoother
                    + "\". Valid smoothers are: "
                    + StringUtils.join(SMOOTHERS, ", ") + ".");
            throw new Termination();
        }

        if (line.hasOption(OPTION_TESTING)) {
            testing = Paths.get(line.getOptionValue(OPTION_TESTING));
            if (!Files.exists(testing)) {
                System.err.println("Testing file \"" + testing
                        + "\" does not exist.");
                throw new Termination();
            }
            if (Files.isDirectory(testing)) {
                System.err.println("Testing file \"" + testing
                        + "\" is a directory.");
                throw new Termination();
            }
            if (!Files.isReadable(testing)) {
                System.err.println("Testing file \"" + testing
                        + "\" is not readable.");
                throw new Termination();
            }
        } else if (line.hasOption(OPTION_CROSSPRODUCT)) {
            crossProductSize =
                    Integer.parseInt(line.getOptionValue(OPTION_CROSSPRODUCT));
        } else {
            System.err
            .println("No test sequences specified. Use either \"-t\" or \"-c\".");
            throw new Termination();
        }
    }

    @Override
    protected void exec() throws IOException {
        Path absoluteDir = corpusDir.resolve("absolute");
        Path continuationDir = corpusDir.resolve("continuation");

        corpus = new Corpus(absoluteDir, continuationDir);

        Estimator estimator = null;
        MaximumLikelihoodEstimator mle = null;
        switch (smoother) {
            case "mle":
                estimator = new MaximumLikelihoodEstimator();
                estimator.setCorpus(corpus);
                calculator = new SkipCalculator(estimator);
                break;
            case "dmle":
                estimator = new MaximumLikelihoodEstimator();
                estimator.setCorpus(corpus);
                calculator = new DeleteCalculator(estimator);
                break;
            case "fmle":
                estimator = new FalseMaximumLikelihoodEstimator();
                estimator.setCorpus(corpus);
                calculator = new DeleteCalculator(estimator);
                break;
            case "cmle":
                estimator = new ContinuationMaximumLikelihoodEstimator();
                estimator.setCorpus(corpus);
                calculator = new SkipCalculator(estimator);
                break;
            case "dcmle":
                estimator = new ContinuationMaximumLikelihoodEstimator();
                estimator.setCorpus(corpus);
                calculator = new DeleteCalculator(estimator);
                break;
            case "bmle":
                mle = new MaximumLikelihoodEstimator();
                estimator = new BackoffEstimator(mle, mle);
                estimator.setCorpus(corpus);
                calculator = new SkipCalculator(estimator);
                break;
            case "dbmle":
                mle = new MaximumLikelihoodEstimator();
                estimator = new BackoffEstimator(mle, mle);
                estimator.setCorpus(corpus);
                calculator = new DeleteCalculator(estimator);

            default:
                // We check for valid smoothers options before, so this should
                // not happen
                throw new IllegalStateException("Missing case statement.");
        }

        if (testing == null) {
            if (crossProductSize == null) {
                throw new IllegalStateException("No testing sequences.");
            } else {
                testing = Files.createTempFile("", "");
                try (BufferedWriter writer =
                        Files.newBufferedWriter(testing,
                                Charset.defaultCharset())) {
                    writeCrossProduct(writer, corpus, crossProductSize);
                }
            }
        }

        try (BufferedReader reader =
                Files.newBufferedReader(testing, Charset.defaultCharset())) {
            int cntZero = 0;
            int cntNonZero = 0;
            double sumPropabilities = 0;
            double entropy = 0;
            double logBase = Math.log(LOG_BASE);

            String sequence;
            while ((sequence = reader.readLine()) != null) {
                double propability = calculator.propability(sequence);

                if (propability == 0) {
                    ++cntZero;
                } else {
                    ++cntNonZero;
                    sumPropabilities += propability;
                    entropy -= Math.log(propability) / logBase;
                    logger.info("Propability({}) = {}", sequence, propability);
                }
            }

            entropy /= cntNonZero;

            logger.info(StringUtils.repeat("-", 80));

            logger.info("Count Zero-Propablity Sequences = %s (%6.2f%%)",
                    cntZero, +(double) cntZero / (cntZero + cntNonZero));
            logger.info("Count Non-Zero-Propability Sequences = %s (%6.2f%%)",
                    cntNonZero, (double) cntNonZero / (cntZero + cntNonZero));
            logger.info("Sum of Propabilities = {}", sumPropabilities);
            logger.info("Entropy = {}", entropy);

            logger.info(StringUtils.repeat("-", 80));
        }
    }

    private static void writeCrossProduct(
            BufferedWriter writer,
            Corpus corpus,
            int length) throws IOException {
        List<String> words = new ArrayList<String>(corpus.getWords());

        for (int i = 0; i != (int) Math.pow(words.size(), length); ++i) {
            writer.write(getNthCrossProductSequence(words, i, length));
            writer.write("\n");
        }
    }

    private static String getNthCrossProductSequence(
            List<String> words,
            int n,
            int length) {
        List<String> sequence = new LinkedList<String>();
        for (int k = 0; k != length; ++k) {
            sequence.add(words.get(n % words.size()));
            n /= words.size();
        }
        Collections.reverse(sequence);
        return StringUtils.join(sequence, " ");
    }

}
