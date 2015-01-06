package de.glmtk.executables;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.AnsiConsole;

import de.glmtk.Constants;
import de.glmtk.Termination;
import de.glmtk.util.ExceptionUtils;
import de.glmtk.util.StringUtils;

/* package */abstract class Executable {
    private static Logger LOGGER = LogManager.getFormatterLogger(Executable.class);
    protected static final String OPTION_HELP_SHORT = "h";
    protected static final String OPTION_HELP_LONG = "help";
    protected static final String OPTION_VERSION_SHORT = "v";
    protected static final String OPTION_VERSION_LONG = "version";

    protected CommandLine line = null;

    protected abstract List<Option> getOptions();

    protected abstract String getUsage();

    protected abstract void exec() throws Exception;

    public void run(String[] args) throws Exception {
        try {
            parseArguments(args);

            configureLogging();
            OUTPUT.enableAnsi();

            // Calling {@link #printLogHeader(String[])} before
            //  {@link #parseArguments(String[])} because
            // {@link GlmtkExecutable} can only add Logger for
            // "<workingdir>/log" after arguments are parsed.
            printLogHeader(args);

            exec();

            printLogFooter();
        } catch (Termination e) {
            if (e.getMessage() != null)
                System.err.println(e.getMessage());
        } catch (Throwable e) {
            LOGGER.error(String.format("Exception %s",
                    ExceptionUtils.getStackTrace(e)));
            OUTPUT.printError(e.getMessage());
            System.out.println(ExceptionUtils.getStackTrace(e));
        }
    }

    protected void configureLogging() {
        LOGGING_HELPER.addFileAppender(CONFIG.getLogDir().resolve(
                Constants.ALL_LOG_FILE_NAME), "FileAll", true);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(Calendar.getInstance().getTime());
        LOGGING_HELPER.addFileAppender(
                CONFIG.getLogDir().resolve(time + ".log"), "FileTimestamp",
                false);
    }

    protected void parseArguments(String[] args) {
        Options options = new Options();
        for (Option option : getOptions())
            options.addOption(option);

        try {
            CommandLineParser parser = new PosixParser();
            line = parser.parse(options, args);
        } catch (ParseException e) {
            throw new Termination(e.getMessage());
        }

        if (line.hasOption(OPTION_VERSION_LONG)) {
            System.out.println("GLMTK (Generalized Language Modeling Toolkit) version 0.1.");
            throw new Termination();
        }

        if (line.hasOption(OPTION_HELP_LONG)) {
            GlmtkHelpFormatter formatter = new GlmtkHelpFormatter();
            formatter.setSyntaxPrefix("Usage: ");
            formatter.setLongOptPrefix(" --");
            formatter.setOptionComparator(new Comparator<Option>() {

                @Override
                public int compare(Option o1,
                                   Option o2) {
                    return getOptions().indexOf(o1) - getOptions().indexOf(o2);
                }

            });

            String header = null;
            String footer = null;

            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                    AnsiConsole.out, Constants.CHARSET))) {
                formatter.printHelp(pw, 80, getUsage(), header, options, 2, 8,
                        footer);
            }
            throw new Termination();
        }
    }

    private void printLogHeader(String[] args) throws IOException, InterruptedException {
        LOGGER.info(StringUtils.repeat("=", 80));
        LOGGER.info(getClass().getSimpleName());

        LOGGER.info(StringUtils.repeat("-", 80));

        // log git commit
        Process gitLogProc = Runtime.getRuntime().exec(
                new String[] {"git", "log", "-1", "--format=%H: %s"}, null,
                CONFIG.getGlmtkDir().toFile());
        gitLogProc.waitFor();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                gitLogProc.getInputStream(), Constants.CHARSET))) {
            String gitCommit = reader.readLine();
            LOGGER.info("Git Commit: %s", gitCommit);
        }

        // log user dir
        LOGGER.info("User Dir: %s", CONFIG.getUserDir());

        // log glmtk dir
        LOGGER.info("Glmtk Dir: %s", CONFIG.getGlmtkDir());

        // log arguments
        LOGGER.info("Arguments: %s", StringUtils.join(args, " "));

        // log config
        LOGGER.info("Config: %s", CONFIG);

        LOGGER.info(StringUtils.repeat("-", 80));
    }

    private void printLogFooter() {
        LOGGER.info("done.");
    }
}
