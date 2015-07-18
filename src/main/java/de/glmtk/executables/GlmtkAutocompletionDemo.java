package de.glmtk.executables;

import static de.glmtk.output.Output.println;
import static de.glmtk.output.Output.printlnError;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.logging.Logger;
import de.glmtk.options.IntegerOption;
import de.glmtk.options.custom.ArgmaxExecutorOption;
import de.glmtk.options.custom.CorpusOption;
import de.glmtk.options.custom.EstimatorOption;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import fi.iki.elonen.NanoHTTPD;

public class GlmtkAutocompletionDemo extends Executable {
    private static final Logger LOGGER = Logger.get(GlmtkAutocompletionDemo.class);

    private class Server extends NanoHTTPD {
        public Server() {
            super(8080);
        }

        @Override
        public void start() throws IOException {
            try {
                super.start();
                println("Running on http://localhost:8080/");
                println("Hit enter to stop.");
                try {
                    System.in.read();
                } catch (Throwable ignore) {
                }
                stop();
            } catch (IOException e) {
                printlnError("Could not start server.");
                throw e;
            }
        }

        @Override
        public Response serve(IHTTPSession session) {
            String msg = "<html><body><h1>Hello from GLMTK!</h1></body></html>\n";
            Map<String, String> parms = session.getParms();
            return newFixedLengthResponse(msg);
        }
    }

    public static void main(String[] args) {
        new GlmtkAutocompletionDemo().run(args);
    }

    private CorpusOption optionCorpus;
    private ArgmaxExecutorOption optionArgmaxExecutorOption;
    private EstimatorOption optionEstimator;
    private IntegerOption optionNGramSize;

    private Path corpus;
    private Path workingDir;
    private String executor;
    private WeightedSumEstimator estimator;
    private int ngramSize;

    @Override
    protected String getExecutableName() {
        return "glmtk-automcompletion-demo";
    }

    @Override
    protected void registerOptions() {
        optionCorpus = new CorpusOption(null, "corpus",
                "Give corpus and maybe working direcotry.");
        optionArgmaxExecutorOption = new ArgmaxExecutorOption("a",
                "argmax-executor", "Executor to use for completion");
        optionEstimator = new EstimatorOption("e", "estimator",
                "Estimator to use for completion.").requireWeightedSum();
        optionNGramSize = new IntegerOption("n", "ngram-size",
                "Max N-Gram length to use for completion.").requireNotZero().requirePositive().defaultValue(
                3);

        commandLine.inputArgs(optionCorpus);
        commandLine.options(optionArgmaxExecutorOption, optionEstimator,
                optionNGramSize);
    }

    @Override
    protected String getHelpHeader() {
        return "Launches webserver for autocompletion demo.";
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

        if (!optionArgmaxExecutorOption.wasGiven())
            throw new CliArgumentException("No executor given, use %s.",
                    optionArgmaxExecutorOption);
        executor = optionArgmaxExecutorOption.getArgmaxExecutor();

        if (!optionEstimator.wasGiven())
            throw new CliArgumentException("No estimator given, use %s.",
                    optionEstimator);
        estimator = (WeightedSumEstimator) optionEstimator.getEstimator();

        ngramSize = optionNGramSize.getInt();
    }

    @Override
    protected void exec() throws Exception {
        Server server = new Server();
        server.start();
    }
}
