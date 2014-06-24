package de.typology.executables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class Glmtk extends Executable {

    private Path corpus = null;

    private static List<Option> optionsOrder;

    private static Options options;

    static {
        //@formatter:off
        Option help    = new Option("h", OPTION_HELP,    false, "Print this message.");
        Option version = new Option("v", OPTION_VERSION, false, "Print the version information and exit.");
        //@formatter:on

        optionsOrder = new ArrayList<Option>();
        optionsOrder.add(help);
        optionsOrder.add(version);

        options = new Options();
        for (Option option : optionsOrder) {
            options.addOption(option);
        }
    }

    private static class OptionComparator<T extends Option > implements
            Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            return optionsOrder.indexOf(o1) - optionsOrder.indexOf(o2);
        }
    }

    public static void main(String[] args) {
        CommandLineParser parser = new PosixParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption(OPTION_VERSION)) {
                System.out
                        .println("GLMTK (generalized language modeling toolkit) version 0.1.");
                return;
            }

            if (line.hasOption(OPTION_HELP)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setSyntaxPrefix("Usage: ");
                formatter.setWidth(80);
                formatter.setOptionComparator(new OptionComparator<Option>());
                formatter.printHelp("glmtk [OPTION]... <CORPUS>", options);
                return;
            }

            if (line.getArgs() == null || line.getArgs().length == 0) {
                System.err.println("glmtk: misssing corpus\n"
                        + "Try 'glmtk-count --help' for more information.");
                return;
            }

            new Glmtk().run(line, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    protected void exec(CommandLine line) {
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
