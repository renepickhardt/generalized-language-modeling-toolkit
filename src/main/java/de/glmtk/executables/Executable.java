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

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.Config;
import de.glmtk.exceptions.Termination;
import de.glmtk.logging.Logger;
import de.glmtk.util.ExceptionUtils;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

/* package */abstract class Executable {
    private static Logger LOGGER = Logger.get(Executable.class);
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
            //TODO: what is happening here?
            OUTPUT.tryToEnableAnsi();
            outputIntialized = true;

            printLogHeader(args);

            exec();
            
            //TODO:move footer outside of try catch block? 
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
            // TODO: other version?  especially the version should not be hardcoded here but rather being pulled from maven pom?
            System.out.println("GLMTK (Generalized Language Modeling Toolkit) version 0.1.");
            // TODO: why do we have situations where we call System.exit(0) and this situation here? What is the difference?
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

        // TODO: what happens if git is not installed? Distributing a standalone jar makes this situation likely
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
