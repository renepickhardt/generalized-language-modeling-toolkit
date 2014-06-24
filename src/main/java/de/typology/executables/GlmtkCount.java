package de.typology.executables;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private static final String OPTION_HELP = "help";

    private static final String OPTION_VERSION = "version";

    private static final String OPTION_OUTPUT = "output";

    private static final String OPTION_MODEL_LENGTH = "model-length";

    private static final String OPTION_COUNT_POS = "count-pos";

    private static final String OPTION_TAG_POS = "tag-pos";

    private static final String OPTION_PATTERNS = "patterns";

    private static final String OPTION_NO_CONTCOUNTS = "no-contcounts";

    private static final String OPTION_KEEP_TEMP = "keep-temp";

    private Path corpus = null;

    private Path output = null;

    private int modelLength = 5;

    private boolean countPos = false;

    private boolean tagPos = false;

    private String patterns = "cmbskp";

    private boolean noContCounts = false;

    private boolean keepTemp = false;

    private static Options options;
    static {
        //@formatter:off
        Option help         = new Option("h",  OPTION_HELP,          false, "Print this message.");
        Option version      = new Option("v",  OPTION_VERSION,       false, "Print the version information and exit.");
        Option output       = new Option("o",  OPTION_OUTPUT,        true,  "Use given directory for output.");
               output.setArgName("OUTPUTDIR");
        Option modelLength  = new Option("n",  OPTION_MODEL_LENGTH,  true,  "Compute n-grams up to model length N.");
               modelLength.setArgName("N");
        Option countPos     = new Option("t",  OPTION_COUNT_POS,     false, "If set, will include counts of part of speeches.");
        Option tagPos       = new Option("T",  OPTION_TAG_POS,       false, "If set, corpus will be part of speech tagged before counting (automatically sets -t).");
        Option patterns     = new Option("p",  OPTION_PATTERNS,      true,  "noskp or cmbskp");
               patterns.setArgName("PATTERNS");
        Option noContCounts = new Option("c",  OPTION_NO_CONTCOUNTS, false, "If set, will not aggregate continuation counts.");
        Option keepTemp     = new Option(null, OPTION_KEEP_TEMP,     false, "If set, will not delete temp files.");
        //@formatter:on

        options = new Options();
        options.addOption(help);
        options.addOption(version);
        options.addOption(output);
        options.addOption(modelLength);
        options.addOption(countPos);
        options.addOption(tagPos);
        options.addOption(patterns);
        options.addOption(noContCounts);
        options.addOption(keepTemp);
    }

    private static Logger logger = LoggerFactory.getLogger(GlmtkCount.class);

    public static void main(String[] args) throws IOException {
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
                formatter
                        .printHelp("glmtk-count [OPTION]... <CORPUS>", options);
                return;
            }

            if (line.getArgs() == null || line.getArgs().length == 0) {
                System.err.println("glmtk-count: missing corpus\n"
                        + "Try 'glmtk-count --help' for more information.");
                return;
            }

            new GlmtkCount().run(line, args);
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
        // TODO: assert corpus is a readable file

        if (line.hasOption(OPTION_OUTPUT)) {
            output = Paths.get(line.getOptionValue(OPTION_OUTPUT));
        } else {
            output = Paths.get(corpus + ".out");
        }
        // TODO: assert that output does not exist

        if (line.hasOption(OPTION_MODEL_LENGTH)) {
            modelLength =
                    Integer.parseInt(line.getOptionValue(OPTION_MODEL_LENGTH));
        }

        if (line.hasOption(OPTION_COUNT_POS)) {
            countPos = true;
        }

        if (line.hasOption(OPTION_TAG_POS)) {
            countPos = true;
            tagPos = true;
        }

        if (line.hasOption(OPTION_PATTERNS)) {
            patterns = line.getOptionValue(OPTION_PATTERNS);
        }

        if (line.hasOption(OPTION_NO_CONTCOUNTS)) {
            noContCounts = true;
        }

        if (line.hasOption(OPTION_KEEP_TEMP)) {
            keepTemp = false;
        }
    }
}
