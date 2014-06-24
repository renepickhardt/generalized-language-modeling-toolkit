package de.typology.executables;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlmtkCount extends Executable {

    private static Logger logger = LoggerFactory.getLogger(GlmtkCount.class);

    public static void main(String[] args) throws IOException {
        Option help = new Option("h", "help", false, "Print this message.");
        Option version =
                new Option("v", "version", false,
                        "Print the version information and exit.");
        Option output =
                new Option("o", "output", true,
                        "Use given directory for output.");
        output.setArgName("OUTPUTDIR");
        Option modelLength =
                new Option("n", "model-length", true,
                        "Compute n-grams up to model length N.");
        modelLength.setArgName("N");
        Option countPos =
                new Option("t", "count-pos", false,
                        "If set, will include counts of part of speeches.");
        Option tagPos =
                new Option(
                        "T",
                        "tag-pos",
                        false,
                        "If set, corpus will be part of speech tagged before counting (automatically sets -t).");
        Option patterns = new Option("p", "patterns", true, "noskp or cmbskp");
        patterns.setArgName("PATTERNS");
        Option noContCounts =
                new Option("c", "no-contcounts", false,
                        "If set, will not aggregate continuation counts.");
        Option keepTemp =
                new Option(null, "keep-temp", false,
                        "If set, will not delete temp files.");

        Options options = new Options();
        options.addOption(help);
        options.addOption(version);
        options.addOption(output);
        options.addOption(modelLength);
        options.addOption(countPos);
        options.addOption(tagPos);
        options.addOption(patterns);
        options.addOption(noContCounts);
        options.addOption(keepTemp);

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("version")) {
                System.out
                        .println("GLMTK (generalized language modeling toolkit) version 0.1.");
                return;
            }

            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter
                        .printHelp("glmtk-count [OPTION]... <CORPUS>", options);
                return;
            }

            if (line.getArgs() == null || line.getArgs().length == 0) {
                System.err.println("glmtk-count: missing corpus\n"
                        + "Try 'glmtk-count --help' for more information.");
                return;
            }

            new GlmtkCount().run(line);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    protected void exec(CommandLine line) {
    }
}
