package de.glmtk.executables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;

import de.glmtk.Logging;
import de.glmtk.Status;
import de.glmtk.Status.TrainingStatus;
import de.glmtk.tagging.PosTagger;

public class GlmtkNew extends Executable {

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

        Path trainingFile = output.resolve("training.txt");
        Path sequencesDir = output.resolve("sequences");
        Path absoluteDir = output.resolve("absolute");
        Path continuationDir = output.resolve("continuation");

        Status status = new Status(output.resolve("status"), corpus);

        // Whether the corpus should be tagged with POS.
        boolean needTrainingPos = true;

        if (needTrainingPos) {
            if (status.getTraining() != TrainingStatus.DONE_WITH_POS) {
                Files.deleteIfExists(trainingFile);
                PosTagger tagger = new PosTagger(config.getModel());
                tagger.tag(corpus, trainingFile);
                status.setTraining(TrainingStatus.DONE_WITH_POS);
            }
        } else if (status.getTraining() == TrainingStatus.NONE) {
            Files.copy(corpus, trainingFile);
            status.setTraining(TrainingStatus.DONE);
        }

        status.update();
    }
}
