/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.executables;

import static com.google.common.base.Throwables.getStackTraceAsString;
import static de.glmtk.logging.Log4jHelper.addLoggingConsoleAppender;
import static de.glmtk.logging.Log4jHelper.addLoggingFileAppender;
import static de.glmtk.logging.Log4jHelper.getLogLevel;
import static de.glmtk.logging.Log4jHelper.initLog4jHelper;
import static de.glmtk.logging.Log4jHelper.setLogLevel;
import static de.glmtk.output.Output.disableOutputFormatting;
import static de.glmtk.output.Output.enableOutputFormatting;
import static de.glmtk.output.Output.println;
import static de.glmtk.output.Output.printlnError;
import static de.glmtk.util.Files.newBufferedReader;
import static de.glmtk.util.StringUtils.join;
import static de.glmtk.util.StringUtils.repeat;
import static de.glmtk.util.ThreadUtils.executeProcess;

import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Config;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.exceptions.Termination;
import de.glmtk.logging.Logger;
import de.glmtk.logging.Logger.Level;
import de.glmtk.options.BooleanOption;
import de.glmtk.options.CommandLine;
import de.glmtk.options.OptionException;

/* package */abstract class Executable {
    // TODO: make --debug and --log options for all executables.

    private static final Logger LOGGER = Logger.get(Executable.class);

    protected BooleanOption optionHelp;
    protected BooleanOption optionVersion;
    protected BooleanOption optionLogConsole;
    protected BooleanOption optionLogDebug;
    protected BooleanOption optionLogTrace;

    protected Config config;
    protected CommandLine commandLine;
    private boolean logConsole;
    private boolean logDebug;
    private boolean logTrace;

    protected abstract String getExecutableName();

    protected abstract void registerOptions();

    protected abstract String getHelpHeader();

    protected abstract String getHelpFooter();

    protected abstract void exec() throws Exception;

    public void run(String[] args) {
        try {
            configureEssentialLogging();
            enableOutputFormatting();
            parseOptions(args);
            configureLogging();
            config = new Config();
            printLogHeader(args);
            exec();
            printLogFooter();
        } catch (Termination e) {
            if (e.getMessage() != null)
                println(e.getMessage());
        } catch (Throwable e) {
            printlnError(e.getMessage());
            LOGGER.error(String.format("Exception %s", getStackTraceAsString(e)));
        }
    }

    protected void parseOptions(String[] args) throws Exception {
        commandLine = new CommandLine();

        optionHelp = new BooleanOption("h", "help", "Print this message.");
        optionVersion = new BooleanOption("v", "version",
                "Print the version information and exit.");
        optionLogConsole = new BooleanOption(null, "log",
                "Logging messages are also written to stdout.");
        optionLogDebug = new BooleanOption(null, "debug",
                "Set log level to DEBUG.");
        optionLogTrace = new BooleanOption(null, "trace",
                "Set log level to TRACE.");

        commandLine.options(optionHelp, optionVersion);
        registerOptions();
        commandLine.options(optionLogConsole, optionLogDebug, optionLogTrace);

        try {
            commandLine.parse(args);
        } catch (OptionException e) {
            throw new CliArgumentException(e.getMessage());
        }

        if (optionHelp.getBoolean()) {
            String helpHeader = getHelpHeader();
            String helpFooter = getHelpFooter();

            System.out.format("%s%s <OPTION...>\n", getExecutableName(),
                    commandLine.getInputArgsLine());
            if (helpHeader != null)
                System.out.println(helpHeader);

            commandLine.help(System.out);

            if (helpFooter != null) {
                System.out.println();
                System.out.println(helpFooter);
            }
            System.out.println();
            System.out.println("For more information, see:");
            System.out.println("https://github.com/renepickhardt/generalized-language-modeling-toolkit/");

            throw new Termination();
        }

        if (optionVersion.getBoolean()) {
            // TODO: other version?  especially the version should not be hardcoded here but rather being pulled from maven pom?
            System.out.println("GLMTK (Generalized Language Modeling Toolkit) version 0.1.");
            throw new Termination();
        }

        logConsole = optionLogConsole.getBoolean();
        logDebug = optionLogDebug.getBoolean();
        logTrace = optionLogTrace.getBoolean();
    }

    /**
     * Makes essential logging configuration:
     * <ul>
     * <li>Setup logging to {@code <LOG_DIR>/all.log}.
     * <li>Setup logging to {@code <LOG_DIR>/<TIMESTAMP>.log}
     * </ul>
     *
     * <p>
     * More sophisticated logging configuration takes place in
     * {@link #configureLogging()}.
     */
    private void configureEssentialLogging() {
        initLog4jHelper(false);

        addLoggingFileAppender(
                GlmtkPaths.LOG_DIR.resolve(Constants.ALL_LOG_FILE_NAME),
                "FileAll", true);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(Calendar.getInstance().getTime());
        addLoggingFileAppender(GlmtkPaths.LOG_DIR.resolve(time + ".log"),
                "FileTimestamp", false);

    }

    /**
     * Makes logging configuration that can't take place in
     * {@link #configureEssentialLogging()}.
     *
     * <p>
     * Reasons for this may be that program arguments may not have been parsed
     * yet, or that the working directory does not yet exists. Executables that
     * wan't to provide own logging configuration should usually override this
     * methods implementation and call it with {@code super.configureLogging()}.
     *
     * <p>
     * There is AFAIK not way to make loggers created here receive log messages
     * that were printed beforehand to the "essential" loggers. Because of this
     * all non-error logging should only occur after this method has completed.
     */
    protected void configureLogging() {
        if (logConsole) {
            // Class Output assumes to be the only source of console output.
            // Logging to console violates this so we disable output formatting.
            disableOutputFormatting();

            addLoggingConsoleAppender(Target.SYSTEM_ERR);
        }

        if (logDebug && getLogLevel().isMoreSpecificThan(Level.DEBUG))
            setLogLevel(Level.DEBUG);
        if (logTrace && getLogLevel().isMoreSpecificThan(Level.TRACE))
            setLogLevel(Level.TRACE);
    }

    private void printLogHeader(String[] args) {
        LOGGER.info(repeat("=", 80));
        LOGGER.info(getClass().getSimpleName());

        LOGGER.info(repeat("-", 80));

        // log git commit
        String gitCommit = "unavailable";
        try {
            Process gitLogProc = Runtime.getRuntime().exec(
                    new String[] {"git", "log", "-1", "--format=%H: %s"}, null,
                    GlmtkPaths.GLMTK_DIR.toFile());

            executeProcess(gitLogProc, Constants.MAX_IDLE_TIME,
                    TimeUnit.MILLISECONDS);

            try (BufferedReader reader = newBufferedReader(
                    gitLogProc.getInputStream(), Constants.CHARSET)) {
                gitCommit = reader.readLine();
            }
        } catch (Throwable e) {
        }
        LOGGER.info("Git Commit: %s", gitCommit);

        // log arguments
        LOGGER.info("Arguments: %s", join(args, " "));

        GlmtkPaths.logStaticPaths();
        config.logConfig();

        LOGGER.info(repeat("-", 80));
    }

    private void printLogFooter() {
        LOGGER.info("done.");
    }
}
