package de.glmtk.executables;

import static de.glmtk.utils.NioUtils.CheckFile.EXISTS;
import static de.glmtk.utils.NioUtils.CheckFile.IS_DIRECTORY;
import static de.glmtk.utils.NioUtils.CheckFile.IS_NO_DIRECTORY;
import static de.glmtk.utils.NioUtils.CheckFile.IS_READABLE;
import static de.glmtk.utils.NioUtils.CheckFile.IS_REGULAR_FILE;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;

import de.glmtk.Glmtk;
import de.glmtk.Model;
import de.glmtk.Termination;
import de.glmtk.querying.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.utils.CountCache;
import de.glmtk.utils.LogUtils;
import de.glmtk.utils.NioUtils;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.Patterns;
import de.glmtk.utils.StatisticalNumberHelper;
import de.glmtk.utils.StringUtils;

public class GlmtkExecutable extends Executable {

    // TODO: API to count all patterns.

    private static final String OPTION_WORKINGDIR = "workingdir";

    private static final String OPTION_MODELSIZE = "modelsize";

    private static final String OPTION_MODEL = "models";

    private static final String OPTION_TESTING = "testing";

    private static List<Option> options;
    static {
        //@formatter:off
        Option help       = new Option("h", OPTION_HELP,       false, "Print this message.");
        Option version    = new Option("v", OPTION_VERSION,    false, "Print the version information and exit.");
        Option workingDir = new Option("w", OPTION_WORKINGDIR, true,  "Working directory.");
        workingDir.setArgName("WORKINGDIR");
        StringBuilder modelOptDesc = new StringBuilder();
        for (Model model : Model.values()) {
            String abbreviation = model.getAbbreviation();
            modelOptDesc.append(abbreviation);
            modelOptDesc.append(StringUtils.repeat(" ", 5 - abbreviation.length()));
            modelOptDesc.append("- ");
            modelOptDesc.append(model.getName());
            modelOptDesc.append(".\n");
        }
        Option modelSize  = new Option("n", OPTION_MODELSIZE,  true,  "MODELSIZE");
        modelSize.setArgName("MODELSIZE");
        Option model      = new Option("m", OPTION_MODEL,      true,  "Can be specified multiple times.\n" + modelOptDesc.toString());
        model.setArgName("MODEL");
        Option testing    = new Option("t", OPTION_TESTING,    true,  "File to take testing sequences for probability and entropy from (can be specified multiple times).");
        testing.setArgName("TESTING");
        //@formatter:on
        options =
                Arrays.asList(help, version, workingDir, modelSize, model,
                        testing);
    }

    private Path corpus = null;

    private Path workingDir = null;

    private int modelSize = 5;

    private Set<Model> models = new HashSet<Model>();

    private Set<Path> testingFiles = new HashSet<Path>();

    public static void main(String[] args) {
        new GlmtkExecutable().run(args);
    }

    @Override
    protected List<Option> getOptions() {
        return options;
    }

    @Override
    protected String getUsage() {
        return "glmtk [OPTION]... <INPUT>";
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);

        if (line.getArgs() == null || line.getArgs().length == 0) {
            throw new Termination("Missing input.\n"
                    + "Try 'glmtk --help' for more information.");
        }

        Path inputArg = Paths.get(line.getArgs()[0]);
        if (!NioUtils.checkFile(inputArg, EXISTS, IS_READABLE)) {
            throw new Termination("Input file/dir '" + inputArg
                    + "' does not exist or is not readable.");
        }

        if (NioUtils.checkFile(inputArg, IS_DIRECTORY)) {
            if (line.hasOption(OPTION_WORKINGDIR)) {
                throw new Termination(
                        "Can't use --"
                                + OPTION_WORKINGDIR
                                + " (-w) argument if using existing working directory as input.");
            }

            workingDir = inputArg;
            getAndCheckCorpusFile(workingDir, "status");
            corpus = getAndCheckCorpusFile(workingDir, "training");
        } else {
            workingDir =
                    line.hasOption(OPTION_WORKINGDIR) ? Paths.get(line
                            .getOptionValue(OPTION_WORKINGDIR)) : Paths
                            .get(inputArg + ".out");
            if (NioUtils.checkFile(workingDir, EXISTS, IS_NO_DIRECTORY)) {
                System.err.println("Working directory '" + workingDir
                        + "' already exists but is not a directory.");
            }

            corpus = inputArg;
        }

        if (line.hasOption(OPTION_MODELSIZE)) {
            boolean exception = false;
            try {
                modelSize =
                        Integer.valueOf(line.getOptionValue(OPTION_MODELSIZE));
            } catch (NumberFormatException e) {
                exception = true;
            }
            if (exception || modelSize <= 0) {
                throw new Termination("Unkown model size '"
                        + line.getOptionValue(OPTION_MODELSIZE)
                        + "'. Need to be a positive integer.");
            }
        }

        if (line.hasOption(OPTION_MODEL)) {
            for (String modelOpt : line.getOptionValues(OPTION_MODEL)) {
                Model model = Model.fromAbbreviation(modelOpt.toUpperCase());
                if (model == null) {
                    throw new Termination("Unkown models option '" + modelOpt
                            + "'.");
                }
                models.add(model);
            }
        }

        if (line.hasOption(OPTION_TESTING)) {
            for (String testingOpt : line.getOptionValues(OPTION_TESTING)) {
                Path path = Paths.get(testingOpt.trim());
                if (!NioUtils.checkFile(path, EXISTS, IS_READABLE,
                        IS_REGULAR_FILE)) {
                    throw new Termination("Testing file '" + path
                            + "' does not exist or is not readable.");
                }
                testingFiles.add(path);
            }
        }
    }

    private Path getAndCheckCorpusFile(Path workingDir, String filename) {
        Path file = workingDir.resolve(filename);
        if (!NioUtils.checkFile(file, EXISTS, IS_READABLE)) {
            throw new Termination(filename + " file '" + file
                    + "' does not exist or is not readable.");
        }
        return file;
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();
        LogUtils.addLocalFileAppender(workingDir.resolve("log"));
    }

    @Override
    protected void exec() throws IOException {
        Glmtk glmtk = new Glmtk(corpus, workingDir);

        boolean needPos = false;

        ProbMode probMode = ProbMode.MARG;

        if (models.isEmpty()) {
            models.add(Model.MODIFIED_KNESER_NEY);
        }

        Set<Pattern> neededPatterns = new HashSet<Pattern>();
        for (Model model : models) {
            Estimator estimator = model.getEstimator();

            neededPatterns.addAll(Patterns.getUsedPatterns(modelSize,
                    estimator, probMode));
            if (needPos) {
                neededPatterns.addAll(Patterns.getPosPatterns(neededPatterns));
            }
        }

        glmtk.count(needPos, neededPatterns);

        if (!testingFiles.isEmpty()) {
            for (Path testingFile : testingFiles) {
                for (Model model : models) {
                    Estimator estimator = model.getEstimator();

                    CountCache countCache =
                            glmtk.getOrCreateTestCountCache(testingFile,
                                    neededPatterns);
                    glmtk.test(testingFile, estimator, probMode, countCache);
                }
            }
        }

        StatisticalNumberHelper.print();
    }
}
