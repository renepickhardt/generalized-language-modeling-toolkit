package de.typology.executables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
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
        return "glmtk [OPTION]... <CORPUS>";
    }

    @Override
    protected String getArgError() {
        return "glmtk: misssing corpus\n"
                + "Try 'glmtk-count --help' for more information.";
    }

    @Override
    protected void exec() {
        readOptions(line);
    }

    private void readOptions(CommandLine line) {
        corpus = Paths.get(line.getArgs()[0]);
        if (!Files.exists(corpus)) {
            throw new IllegalStateException("Given corpus \"" + corpus
                    + "\" does not exist.");
        }
        if (!Files.isDirectory(corpus)) {
            throw new IllegalStateException("Given corpus \"" + corpus
                    + "\" is not a directory.");
        }
        if (!Files.isReadable(corpus)) {
            throw new IllegalStateException("Given corpus \"" + corpus
                    + "\" is not readable.");
        }
    }

}
