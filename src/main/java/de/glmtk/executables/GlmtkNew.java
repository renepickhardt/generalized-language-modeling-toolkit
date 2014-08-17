package de.glmtk.executables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.Status.TrainingStatus;
import de.glmtk.counting.AbsoluteCounter;
import de.glmtk.counting.ContinuationCounter;
import de.glmtk.counting.Tagger;
import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;
import de.glmtk.utils.Logging;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

public class GlmtkNew extends Executable {

    private static final Logger LOGGER = LogManager.getLogger(GlmtkNew.class);

    private static final String OPTION_OUTPUT = "output";

    private static List<Option> options;
    static {
        //@formatter:off
        Option help    = new Option("h", OPTION_HELP,    false, "Print this message.");
        Option version = new Option("v", OPTION_VERSION, false, "Print the version information and exit.");
        Option output  = new Option("o", OPTION_OUTPUT,  true,  "Use given directory for output.");
        output.setArgName("OUTPUTDIR");
        //@formatter:on
        options = Arrays.asList(help, version, output);
    }

    private Path corpus = null;

    private Path output = null;

    public static void main(String[] args) {
        new GlmtkNew().run(args);
    }

    @Override
    protected List<Option> getOptions() {
        return options;
    }

    @Override
    protected String getUsage() {
        return "glmtk-new [OPTION]... <CORPUS>";
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();
        Logging.addLocalFileAppender(output.resolve("log"));
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);

        if (line.getArgs() == null || line.getArgs().length == 0) {
            System.err.println("Missing corpus\n"
                    + "Try 'glmtk-new --help' for more information.");
            throw new Termination();
        }

        corpus = Paths.get(line.getArgs()[0]);
        if (!Files.exists(corpus)) {
            System.err.println("Corpus '" + corpus + "' does not exist.");
            throw new Termination();
        }
        if (!Files.isReadable(corpus)) {
            System.err.println("Corpus '" + corpus + "' is not readable.");
            throw new Termination();
        }
        if (Files.isDirectory(corpus)) {
            // TODO: Allow corpus to be directory.
            System.err.println("Specifying a corpus "
                    + "as a directory is not supported yet.");
            throw new Termination();
        }

        if (line.hasOption(OPTION_OUTPUT)) {
            output = Paths.get(line.getOptionValue(OPTION_OUTPUT));
        } else {
            output = Paths.get(corpus + ".out");
        }
        if (Files.exists(output) && !Files.isDirectory(output)) {
            System.err.println("Output file '" + output
                    + "' already exists but is not a directory.");
        }
    }

    @Override
    protected void exec() throws IOException {
        if (!Files.exists(output)) {
            Files.createDirectories(output);
        }

        Path trainingFile = output.resolve("training");
        Path absoluteDir = output.resolve("absolute");
        Path absoluteTmpDir = output.resolve("absolute.tmp");
        Path continuationDir = output.resolve("continuation");
        Path continuationTmpDir = output.resolve("continuation.tmp");

        Status status =
                new Status(output.resolve("status"),
                        output.resolve("status.tmp"), corpus);
        status.logStatus();

        // TODO: check file system if status is accurate.
        // TODO: update status with smaller increments (each completed pattern).

        // Request /////////////////////////////////////////////////////////////

        // Whether the corpus should be tagged with POS.
        boolean needToTagTraining = true;
        // Absolute Patterns we need
        Set<Pattern> neededAbsolutePatterns =
                Pattern.getCombinations(5, Arrays.asList(PatternElem.CNT,
                        PatternElem.SKP, PatternElem.POS));
        // Continuation Patterns we need
        System.out.println(neededAbsolutePatterns.size());
        Set<Pattern> neededContinuationPatterns =
                Pattern.replaceTargetWithElems(neededAbsolutePatterns,
                        PatternElem.SKP,
                        Arrays.asList(PatternElem.WSKP, PatternElem.PSKP));
        System.out.println(neededContinuationPatterns.size());

        // Add patterns to absolute that are needed to generate continuation.
        for (Pattern pattern : neededContinuationPatterns) {
            Pattern sourcePattern =
                    Pattern.getContinuationSourcePattern(pattern);
            if (sourcePattern.isAbsolute()) {
                neededAbsolutePatterns.add(sourcePattern);
            }
        }

        LOGGER.debug("Request {}", StringUtils.repeat("-", 80 - 8));
        LOGGER.debug("needToTagTraning           = {}", needToTagTraining);
        LOGGER.debug("neededAbsolutePatterns     = {}", neededAbsolutePatterns);
        LOGGER.debug("neededContinuationPatterns = {}",
                neededContinuationPatterns);

        // Training / Tagging //////////////////////////////////////////////////

        // TODO: doesn't detect the setting that user changed from untagged
        // training file, to tagged file with same corpus.
        if (needToTagTraining) {
            if (status.getTraining() == TrainingStatus.DONE_WITH_POS) {
                LOGGER.info("Detected tagged training already present, skipping tagging.");
            } else {
                Files.deleteIfExists(trainingFile);
                Tagger tagger =
                        new Tagger(config.getUpdateInterval(),
                                config.getModel());
                tagger.tag(corpus, trainingFile);
                status.setTraining(TrainingStatus.DONE_WITH_POS);
            }
        } else {
            if (status.getTraining() != TrainingStatus.NONE) {
                LOGGER.info("Detected training already present, skipping copying training.");
            } else {
                Files.deleteIfExists(trainingFile);
                Files.copy(corpus, trainingFile);
                status.setTraining(TrainingStatus.DONE);
            }
        }

        // Absolute ////////////////////////////////////////////////////////////

        AbsoluteCounter absoluteCounter =
                new AbsoluteCounter(neededAbsolutePatterns,
                        config.getNumberOfCores(), config.getUpdateInterval());
        absoluteCounter
                .count(trainingFile, absoluteDir, absoluteTmpDir, status);

        // Continuation ////////////////////////////////////////////////////////

        ContinuationCounter continuationCounter =
                new ContinuationCounter(neededContinuationPatterns,
                        config.getNumberOfCores(), config.getUpdateInterval());
        continuationCounter.count(absoluteDir, continuationDir,
                continuationTmpDir, status);

        // Used for debugging. Will only print output if averages are added
        // somewhere else in the code.
        StatisticalNumberHelper.print();
    }

}
