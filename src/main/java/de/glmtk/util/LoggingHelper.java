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

package de.glmtk.util;

import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import de.glmtk.Constants;

public enum LoggingHelper {
    LOGGING_HELPER;

    private static final String loggingPattern = "%date{yyyy-MM-dd HH:mm:ss} [%-5level]  %-100msg  [%class{1}#%method:%line] %thread (%logger)%n";

    /**
     * Log4j LoggerContext.
     */
    private LoggerContext loggerContext;

    /**
     * Log4j Configuration.
     */
    private Configuration Configuration;

    /**
     * Serializable Log4j Root Logger Configuration.
     */
    private LoggerConfig loggerconfig;

    private Layout<String> layout;

    private LoggingHelper() {
        loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration = loggerContext.getConfiguration();
        loggerconfig = Configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        layout = PatternLayout.createLayout(loggingPattern, Configuration,
                null, Constants.CHARSET, true, true, null, null);
    }

    public Level getLogLevel() {
        return loggerconfig.getLevel();
    }

    public void setLogLevel(Level level) {
        loggerconfig.setLevel(level);

        loggerContext.updateLoggers();
    }

    public void addConsoleAppender(Target target) {
        Appender consoleApender = ConsoleAppender.createAppender(layout, null,
                target.toString(), "Output", "true", "false");
        consoleApender.start();
        Configuration.addAppender(consoleApender);
        loggerconfig.addAppender(consoleApender, null, null);

        loggerContext.updateLoggers();
    }

    /**
     * Adds logging to given {@code logFile}.
     *
     * @param logFile
     *            The file to log to.
     * @param name
     *            Log4j Internal Appender Name.
     * @param append
     *            If {@code true} log messages are appended to the file. If
     *            {@code false} log message replace file contents.
     */
    public void addFileAppender(Path logFile,
                                String name,
                                boolean append) {
        Appender fileLocalAppender = FileAppender.createAppender(
                logFile.toString(), Boolean.toString(append), "false", name,
                "false", "false", "true", "8192", layout, null, "false",
                "false", Configuration);
        fileLocalAppender.start();
        Configuration.addAppender(fileLocalAppender);
        loggerconfig.addAppender(fileLocalAppender, null, null);

        loggerContext.updateLoggers();
    }
}
