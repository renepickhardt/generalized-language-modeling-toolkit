package de.typology.executables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;

import de.typology.smoothing.ContinuationMaximumLikelihoodEstimator;
import de.typology.smoothing.Corpus;
import de.typology.smoothing.DeleteCalculator;
import de.typology.smoothing.Estimator;
import de.typology.smoothing.FalseMaximumLikelihoodEstimator;
import de.typology.smoothing.MaximumLikelihoodEstimator;
import de.typology.smoothing.PropabilityCalculator;
import de.typology.smoothing.SkipCalculator;
import de.typology.utils.StringUtils;

public class Glmtk extends Executable {

    private static final List<String> SMOOTHERS = Arrays.asList("mle", "dmle",
            "fmle", "cmle", "dcmle");

    private static final String OPTION_SMOOTHER = "smoother";

    private static List<Option> options;
    static {
        //@formatter:off
        Option help     = new Option("h", OPTION_HELP,     false, "Print this message.");
        Option version  = new Option("v", OPTION_VERSION,  false, "Print the version information and exit.");
        Option smoother = new Option("s", OPTION_SMOOTHER, true,  StringUtils.join(SMOOTHERS, ", ") + ".");
               smoother.setArgName("SMOOTHER");
        //@formatter:on
        options = Arrays.asList(help, version, smoother);
    }

    private Path corpusDir = null;

    private Corpus corpus = null;

    private String smoother = "mle";

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
    }

    @Override
    protected void exec() throws IOException {
        Path absoluteDir = corpusDir.resolve("absolute");
        Path continuationDir = corpusDir.resolve("continuation");

        Corpus corpus = new Corpus(absoluteDir, continuationDir, "\t");

        Estimator estimator = null;
        switch (smoother) {
            case "mle":
                estimator = new MaximumLikelihoodEstimator(corpus);
                calculator = new SkipCalculator(estimator);
                break;
            case "dmle":
                estimator = new MaximumLikelihoodEstimator(corpus);
                calculator = new DeleteCalculator(estimator);
                break;
            case "fmle":
                estimator = new FalseMaximumLikelihoodEstimator(corpus);
                calculator = new DeleteCalculator(estimator);
                break;
            case "cmle":
                estimator = new ContinuationMaximumLikelihoodEstimator(corpus);
                calculator = new SkipCalculator(estimator);
                break;
            case "dcmle":
                estimator = new ContinuationMaximumLikelihoodEstimator(corpus);
                calculator = new DeleteCalculator(estimator);
                break;

            default:
                // We check for valid smoothers options before, so this should
                // not happen
                throw new IllegalStateException("Missing case statement.");
        }
    }

}
