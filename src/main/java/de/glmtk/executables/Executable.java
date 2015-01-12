package de.glmtk.executables;

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Config;
import de.glmtk.exceptions.Termination;
import de.glmtk.util.ExceptionUtils;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

/* package */abstract class Executable {
    private static Logger LOGGER = LogManager.getFormatterLogger(Executable.class);
    protected static final String OPTION_HELP_SHORT = "h";
    protected static final String OPTION_HELP_LONG = "help";
    protected static final String OPTION_VERSION_SHORT = "v";
    protected static final String OPTION_VERSION_LONG = "version";

    protected Config config = null;
    protected CommandLine line = null;
    private boolean outputIntialized = false;

    protected abstract List<Option> getOptions();

    protected abstract String getHelpHeader();

    protected abstract String getHelpFooter();

    protected abstract void exec() throws Exception;

    public void run(String[] args) {
        try {
            parseArguments(args);

            configureLogging();

            config = new Config();

            OUTPUT.initialize(config);
            OUTPUT.tryToEnableAnsi();
            outputIntialized = true;

            printLogHeader(args);

            exec();

            printLogFooter();
        } catch (Termination e) {
            if (e.getMessage() != null)
                System.err.println(e.getMessage());
        } catch (Throwable e) {
            if (outputIntialized)
                OUTPUT.printError(e.getMessage());
            else
                System.err.println(e.getMessage());
            LOGGER.error(String.format("Exception %s",
                    ExceptionUtils.getStackTrace(e)));
        }
    }

    protected void configureLogging() {
        LOGGING_HELPER.addFileAppender(
                GlmtkPaths.LOG_DIR.resolve(Constants.ALL_LOG_FILE_NAME),
                "FileAll", true);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(Calendar.getInstance().getTime());
        LOGGING_HELPER.addFileAppender(
                GlmtkPaths.LOG_DIR.resolve(time + ".log"), "FileTimestamp",
                false);
    }

    protected void parseArguments(String[] args) throws Exception {
        Options options = new Options();
        for (Option option : getOptions())
            options.addOption(option);

        CommandLineParser parser = new PosixParser();
        line = parser.parse(options, args);

        if (line.hasOption(OPTION_VERSION_LONG)) {
            System.out.println("GLMTK (Generalized Language Modeling Toolkit) version 0.1.");
            throw new Termination();
        }

        if (line.hasOption(OPTION_HELP_LONG)) {
            System.out.print(getHelpHeader());

            PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out,
                    Constants.CHARSET));
            //            GlmtkHelpFormatter formatter = new GlmtkHelpFormatter();
            HelpFormatter formatter = new HelpFormatter();
            formatter.setLongOptPrefix(" --");
            formatter.setOptionComparator(new Comparator<Option>() {
                @Override
                public int compare(Option o1,
                                   Option o2) {
                    return getOptions().indexOf(o1) - getOptions().indexOf(o2);
                }
            });
            formatter.printOptions(pw, 80, options, 2, 2);
            pw.flush();
            // do not close stream to System.out

            System.out.print(getHelpFooter());
            throw new Termination();
        }
    }

    private void printLogHeader(String[] args) {
        LOGGER.info(StringUtils.repeat("=", 80));
        LOGGER.info(getClass().getSimpleName());

        LOGGER.info(StringUtils.repeat("-", 80));

        // log git commit
        String gitCommit = "unavailable";
        try {
            Process gitLogProc = Runtime.getRuntime().exec(
                    new String[] {"git", "log", "-1", "--format=%H: %s"}, null,
                    GlmtkPaths.GLMTK_DIR.toFile());
            ThreadUtils.executeProcess(gitLogProc, Constants.MAX_IDLE_TIME,
                    TimeUnit.MILLISECONDS);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(gitLogProc.getInputStream(),
                            Constants.CHARSET))) {
                gitCommit = reader.readLine();
            }
        } catch (Throwable e) {
        }
        LOGGER.info("Git Commit: %s", gitCommit);

        // log arguments
        LOGGER.info("Arguments: %s", StringUtils.join(args, " "));

        GlmtkPaths.logStaticPaths();
        config.logConfig();

        LOGGER.info(StringUtils.repeat("-", 80));
    }

    private void printLogFooter() {
        LOGGER.info("done.");
    }
}
