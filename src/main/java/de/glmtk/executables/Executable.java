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

import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;
import static de.glmtk.util.NioUtils.CheckFile.EXISTS;
import static de.glmtk.util.NioUtils.CheckFile.IS_READABLE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Config;
import de.glmtk.exceptions.CliArgumentException;
import de.glmtk.exceptions.Termination;
import de.glmtk.logging.Logger;
import de.glmtk.options.BooleanOption;
import de.glmtk.options.OptionException;
import de.glmtk.options.OptionManager;
import de.glmtk.util.ExceptionUtils;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

/* package */abstract class Executable {
    // TODO: make --debug and --log options for all executables.

    private static final Logger LOGGER = Logger.get(Executable.class);

    protected BooleanOption optionHelp;
    protected BooleanOption optionVersion;
    protected BooleanOption optionLogConsole;
    protected BooleanOption optionLogDebug;

    protected Config config;
    protected OptionManager optionManager;
    private boolean outputIntialized = false;

    protected abstract String getExecutableName();

    protected abstract void options();

    protected abstract String getHelpHeader();

    protected abstract String getHelpFooter();

    protected abstract void exec() throws Exception;

    public void run(String[] args) {
        try {
            parseOptions(args);

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

    protected void parseOptions(String[] args) throws Exception {
        optionManager = new OptionManager();

        optionHelp = new BooleanOption("h", "help", "Print this message.");
        optionVersion = new BooleanOption("v", "version",
                "Print the version information and exit.");
        optionLogConsole = new BooleanOption(null, "log",
                "Logging messages are also written to stdout.");
        optionLogDebug = new BooleanOption(null, "debug",
                "Set log level to DEBUG.");

        optionManager.register(optionHelp, optionVersion);
        options();
        optionManager.register(optionLogConsole, optionLogDebug);

        try {
            optionManager.parse(args);
        } catch (OptionException e) {
            throw new CliArgumentException(e.getMessage(), e);
        }

        if (optionHelp.getBoolean()) {
            // getHelpHeader()
            optionManager.help(System.out);
            // getHelpFooter()
            throw new Termination();
        }

        if (optionVersion.getBoolean()) {
            // TODO: other version?  especially the version should not be hardcoded here but rather being pulled from maven pom?
            System.out.println("GLMTK (Generalized Language Modeling Toolkit) version 0.1.");
            throw new Termination();
        }

        //        Options options = new Options();
        //        for (Option option : getOptions())
        //            options.addOption(option);
        //
        //        CommandLineParser parser = new PosixParser();
        //        line = parser.parse(options, args);
        //
        //        if (line.hasOption(OPTION_VERSION_LONG)) {
        //        }
        //
        //        if (line.hasOption(OPTION_HELP_LONG)) {
        //            System.out.print(getHelpHeader());
        //
        //            PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out,
        //                    Constants.CHARSET));
        //            //            GlmtkHelpFormatter formatter = new GlmtkHelpFormatter();
        //            HelpFormatter formatter = new HelpFormatter();
        //            formatter.setLongOptPrefix(" --");
        //            formatter.setOptionComparator(new Comparator<Option>() {
        //                @Override
        //                public int compare(Option o1,
        //                                   Option o2) {
        //                    return getOptions().indexOf(o1) - getOptions().indexOf(o2);
        //                }
        //            });
        //            formatter.printOptions(pw, 80, options, 2, 2);
        //            pw.flush();
        //            // do not close stream to System.out
        //
        //            System.out.print(getHelpFooter());
        //            throw new Termination();
        //        }
    }

    protected Path parseInputArg() {
        //        if (line.getArgList() == null || line.getArgList().size() != 1) {
        //            String error;
        //            if (line.getArgList().size() == 0)
        //                error = "Missing input.\n";
        //            else
        //                error = String.format("Incorrect input: %s%n",
        //                        StringUtils.join(line.getArgList(), " "));
        //            throw new CliArgumentException(error + "Try '"
        //                    + getExecutableName() + " --help' for more information.");
        //        }
        //
        //        Path inputArg = Paths.get(line.getArgs()[0]);
        //        if (!NioUtils.checkFile(inputArg, EXISTS, IS_READABLE))
        //            throw new CliArgumentException(String.format(
        //                    "Input file/dir '%s' does not exist or is not readable.",
        //                    inputArg));
        //        return inputArg;
        return Paths.get("sup");
    }

    protected Path getAndCheckFile(String filename) throws IOException {
        Path file = Paths.get(filename);
        if (!NioUtils.checkFile(file, EXISTS, IS_READABLE))
            throw new IOException(String.format(
                    "File '%s' does not exist or is not readable.", filename));
        return file;
    }

    protected Path getWorkingDirFile(Path workingDir,
                                     String filename) throws IOException {
        Path file = workingDir.resolve(filename);
        if (!NioUtils.checkFile(file, EXISTS, IS_READABLE))
            throw new IOException(String.format(
                    "%s file '%s' does not exist or is not readable.",
                    filename, file));
        return file;
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
