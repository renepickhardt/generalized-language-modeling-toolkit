package de.glmtk.executables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;

import de.glmtk.Glmtk;
import de.glmtk.Model;
import de.glmtk.Termination;
import de.glmtk.utils.Logging;
import de.glmtk.utils.StatisticalNumberHelper;

public class GlmtkExecutable extends Executable {

    private static final String OPTION_WORKINGDIR = "workingdir";

    private static final String OPTION_MODEL = "model";

    private static final String OPTION_TESTING = "testing";

    private static List<Option> options;
    static {
        //@formatter:off
        Option help       = new Option("h", OPTION_HELP,       false, "Print this message.");
        Option version    = new Option("v", OPTION_VERSION,    false, "Print the version information and exit.");
        Option workingDir = new Option("w", OPTION_WORKINGDIR, true,  "Working directory.");
        workingDir.setArgName("WORKINGDIR");
        Option model      = new Option("m", OPTION_MODEL,      true,  "KN  - Kneser Ney\n" +
                "MKN - Modified Kneser Ney\n" +
                "GLM - Generalized Language Model");
        model.setArgName("MODEL");
        Option testing    = new Option("t", OPTION_TESTING,    true,  "File to take testing sequences for probability and entropy from (can be specified multiple times).");
        testing.setArgName("TESTING");
        //@formatter:on
        options = Arrays.asList(help, version, workingDir, model, testing);
    }

    private Glmtk glmtk = new Glmtk();

    private Path workingDir = null;

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
            System.err.println("Missing input.\n"
                    + "Try 'glmtk --help' for more information.");
            throw new Termination();
        }

        Path inputArg = Paths.get(line.getArgs()[0]);
        if (!(Files.exists(inputArg) && Files.isReadable(inputArg))) {
            System.err.println("Input file/dir '" + inputArg
                    + "' does not exist or is not readable.");
            throw new Termination();
        }

        Path corpus = null, workingDir = null;
        if (Files.isDirectory(inputArg)) {
            if (line.hasOption(OPTION_WORKINGDIR)) {
                System.err
                .println("Can't use --"
                                + OPTION_WORKINGDIR
                                + " (-w) argument if using existing working directory as input.");
                throw new Termination();
            }

            workingDir = inputArg;
            getAndCheckCorpusFile(workingDir, "status");
            corpus = getAndCheckCorpusFile(workingDir, "training");
        } else {
            if (line.hasOption(OPTION_WORKINGDIR)) {
                workingDir = Paths.get(line.getOptionValue(OPTION_WORKINGDIR));
            } else {
                workingDir = Paths.get(inputArg + ".out");
            }
            if (Files.exists(workingDir) && !Files.isDirectory(workingDir)) {
                System.err.println("Working directory '" + workingDir
                        + "' already exists but is not a directory.");
            }
        }
        glmtk.setCorpus(corpus);
        glmtk.setWorkingDir(workingDir);
        this.workingDir = workingDir;

        if (line.hasOption(OPTION_MODEL)) {
            switch (line.getOptionValue(OPTION_MODEL).toUpperCase()) {
                case "KN":
                    glmtk.setModel(Model.KNESER_NEY);
                    break;
                case "MKN":
                    glmtk.setModel(Model.MODIFIED_KNESER_NEY);
                    break;
                case "GLM":
                    glmtk.setModel(Model.GENERALIZED_LANGUAGE_MODEL);
                    break;

                default:
                    System.err.println("Unkown model option '"
                            + line.getOptionValue(OPTION_MODEL) + "'.");
                    throw new Termination();
            }
        }

        if (line.hasOption(OPTION_TESTING)) {
            for (String testingFile : line.getOptionValues(OPTION_TESTING)) {
                glmtk.addTestingFile(Paths.get(testingFile.trim()));
            }
        }
    }

    private Path getAndCheckCorpusFile(Path workingDir, String filename) {
        Path file = workingDir.resolve(filename);
        if (!(Files.exists(file) && Files.isReadable(file))) {
            System.err.println(filename + " file '" + file
                    + "' does not exist or is not readable.");
            throw new Termination();
        }
        return file;
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();
        Logging.addLocalFileAppender(workingDir.resolve("log"));
    }

    @Override
    protected void exec() throws IOException {
        glmtk.count();

        // Testing /////////////////////////////////////////////////////////////

        //        if (!testing.isEmpty()) {
        //            testing(absoluteDir, continuationDir, testingDir);
        //        }

        // Used for debugging. Will only print output if averages are added
        // somewhere else in the code.
        StatisticalNumberHelper.print();
    }

    //    private void
    //    testing(Path absoluteDir, Path continuationDir, Path testingDir)
    //            throws IOException {
    //        Files.createDirectories(testingDir);
    //
    //        LOGGER.info("Loading counts into memory...");
    //        CountCache countCache = new CountCache(absoluteDir, continuationDir);
    //
    //        Estimator estimator = null;
    //        switch (model) {
    //            case MODIFIED_KNESER_NEY:
    //                estimator = Estimators.INTERPOL_ABS_DISCOUNT_MLE_REC;
    //                //estimator = Estimators.MODIFIED_KNESER_NEY_ESIMATOR;
    //                break;
    //            default:
    //                throw new UnsupportedOperationException();
    //        }
    //        estimator.setCorpus(countCache);
    //
    //        NGramProbabilityCalculator calculator =
    //                new NGramProbabilityCalculator();
    //        calculator.setProbMode(ProbMode.MARG);
    //        calculator.setEstimator(estimator);
    //
    //        for (Path testingFile : testing) {
    //            SimpleDateFormat format =
    //                    new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss");
    //            Path outputFile =
    //                    testingDir.resolve(testingFile.getFileName()
    //                            + format.format(new Date()));
    //            Files.deleteIfExists(outputFile);
    //
    //            LOGGER.info("Testing File: '%s' -> '%s'.", testingFile, outputFile);
    //
    //            try (BufferedReader reader =
    //                    Files.newBufferedReader(testingFile,
    //                            Charset.defaultCharset());
    //                    BufferedWriter writer =
    //                            Files.newBufferedWriter(outputFile,
    //                                    Charset.defaultCharset())) {
    //                int cntZero = 0;
    //                int cntNonZero = 0;
    //                double sumProbabilities = 0;
    //                double entropy = 0;
    //                double logBase = Math.log(Constants.LOG_BASE);
    //
    //                String line;
    //                while ((line = reader.readLine()) != null) {
    //                    double probability =
    //                            calculator.probability(StringUtils.splitAtChar(
    //                                    line, ' '));
    //
    //                    if (probability == 0) {
    //                        ++cntZero;
    //                    } else {
    //                        ++cntNonZero;
    //                        sumProbabilities += probability;
    //                        entropy -= Math.log(probability) / logBase;
    //                    }
    //
    //                    writer.append(line);
    //                    writer.append('\t');
    //                    writer.append(((Double) probability).toString());
    //                    writer.append('\n');
    //                }
    //
    //                LOGGER.info("Count Zero-Propablity Sequences = %s (%6.2f%%)",
    //                        cntZero, (double) cntZero / (cntZero + cntNonZero)
    //                                * 100);
    //                LOGGER.info(
    //                        "Count Non-Zero-Propability Sequences = %s (%6.2f%%)",
    //                        cntNonZero, (double) cntNonZero
    //                                / (cntZero + cntNonZero) * 100);
    //                LOGGER.info("Sum of Propabilities = %s", sumProbabilities);
    //                LOGGER.info("Entropy = %s", entropy);
    //
    //            }
    //        }
    //    }
}
