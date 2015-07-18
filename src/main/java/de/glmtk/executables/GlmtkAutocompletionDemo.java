package de.glmtk.executables;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static de.glmtk.output.Output.println;
import static de.glmtk.output.Output.printlnError;
import static de.glmtk.util.StringUtils.split;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.GlmtkPaths;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.cache.CacheSpecification.CacheImplementation;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.logging.Logger;
import de.glmtk.options.IntegerOption;
import de.glmtk.options.custom.ArgmaxExecutorOption;
import de.glmtk.options.custom.CorpusOption;
import de.glmtk.options.custom.EstimatorOption;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor;
import de.glmtk.querying.argmax.ArgmaxQueryExecutor.ArgmaxResult;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.util.StringUtils;
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
            try {
                String uri = session.getUri();
                switch (uri) {
                    case "/complete":
                        return serverCompletion(session);

                    default:
                        return serveDemo(session);
                }
            } catch (Throwable e) {
                String stackTrace = getStackTraceAsString(e);
                printlnError(stackTrace);
                return newFixedLengthResponse(INTERNAL_ERROR, "text/plain",
                        stackTrace);
            }
        }

        public Response serveDemo(IHTTPSession session) throws IOException {
            String demoHtml = new String(
                    Files.readAllBytes(GlmtkPaths.GLMTK_DIR.resolve(
                            Constants.MAIN_RESOURCES_DIR).resolve(
                                    "autocompletion-demo.html")), Constants.CHARSET);
            Response response = newFixedLengthResponse(OK,
                    "application/xhtml+xml", demoHtml);
            return response;
        }

        public Response serverCompletion(IHTTPSession session) {
            StringBuilder msg = new StringBuilder();
            msg.append("{\n");

            // Parse query parameters.
            Map<String, String> params = session.getParms();
            String history = getRelevantHistory(params.get("history"));
            String prefix = params.get("prefix");
            if (prefix == null)
                prefix = "";
            int numResults;
            try {
                numResults = Integer.parseInt(params.get("numResults"));
            } catch (NumberFormatException e) {
                numResults = 5;
            }

            // Output query parameters.
            msg.append("  \"history\": \"" + history + "\",\n");
            msg.append("  \"prefix\": \"" + prefix + "\",\n");
            msg.append("  \"numResults\": " + numResults + ",\n");

            // Build completions.
            List<ArgmaxResult> completions = null;
            if (history != null)
                completions = argmaxQueryExecutor.queryArgmax(history, prefix,
                        numResults);

            // Output completions
            msg.append("  \"completion\": ");
            if (completions == null)
                msg.append("null\n");
            else {
                msg.append("[\n");
                boolean first = true;
                for (ArgmaxResult completion : completions) {
                    if (first)
                        first = false;
                    else
                        msg.append(",\n");

                    msg.append("    {\n");
                    msg.append("      \"completion\": \""
                            + completion.getSequence().replace("\"", "\\\"")
                            + "\",\n");
                    msg.append("      \"probability\": \""
                            + format("%e", completion.getProbability())
                            + "\"\n");
                    msg.append("    }");
                }
                msg.append("\n  ]\n");
            }

            msg.append("}\n");
            Response response = newFixedLengthResponse(OK, "application/json",
                    msg.toString());
            return response;
        }

        private String getRelevantHistory(String history) {
            if (history == null)
                return "";
            List<String> split = split(history, ' ');
            split = split.subList(max(split.size() - ngramSize + 1, 0),
                    split.size());
            return StringUtils.join(split, ' ');
        }

        private int max(int a,
                        int b) {
            if (a > b)
                return a;
            return b;
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
    private ArgmaxQueryExecutor argmaxQueryExecutor;

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
        Glmtk glmtk = new Glmtk(config, corpus, workingDir);

        CacheSpecification cacheSpec = estimator.getRequiredCache(ngramSize);
        cacheSpec.withProgress();
        cacheSpec.withWords().withCounts(Patterns.getMany("x")); // FIXME: Refacotr this

        Set<Pattern> requiredPatterns = cacheSpec.getRequiredPatterns();
        requiredPatterns.add(Patterns.get("x1111x")); // FIXME: Refactor this

        GlmtkPaths paths = glmtk.getPaths();

        CompletionTrieCache sortedAccessCache = (CompletionTrieCache) cacheSpec.withCacheImplementation(
                CacheImplementation.COMPLETION_TRIE).build(paths);
        estimator.setCache(sortedAccessCache);
        argmaxQueryExecutor = ArgmaxExecutorOption.argmaxQueryExecutorFromString(
                executor, estimator, sortedAccessCache, sortedAccessCache);

        Server server = new Server();
        server.start();
    }
}
