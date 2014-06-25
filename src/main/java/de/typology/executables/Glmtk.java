package de.typology.executables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.Option;

public class Glmtk extends Executable {

    private static List<Option> options;
    static {
        //@formatter:off
        Option help    = new Option("h", OPTION_HELP,    false, "Print this message.");
        Option version = new Option("v", OPTION_VERSION, false, "Print the version information and exit.");
        //@formatter:on
        options = Arrays.asList(help, version);
    }

    private Path corpus = null;

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
            corpus = Paths.get(".");
        } else {
            corpus = Paths.get(line.getArgs()[0]);
        }
        if (!Files.exists(corpus)) {
            System.err.println("Corpus \"" + corpus + "\" does not exist.");
            throw new Termination();
        }
        if (!Files.isDirectory(corpus)) {
            System.err.println("Corpus \"" + corpus + "\" is not a directory.");
            throw new Termination();
        }
        if (!Files.isReadable(corpus)) {
            System.err.println("Corpus \"" + corpus + "\" is not readable.");
            throw new Termination();
        }
    }

    @Override
    protected void exec() {
    }

}
