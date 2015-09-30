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

package de.glmtk.logging;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.glmtk.util.StringUtils.join;

import java.nio.file.Path;

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
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.logging.Logger.Level;


public class Log4jHelper {
    private Log4jHelper() {}

    private static final String LOGGING_PATTERN =
        "%date{yyyy-MM-dd HH:mm:ss} [%-5level]  %-100msg  [%class{1}#%method:%line] %thread (%logger)%n";

    /**
     * Buffer size to use for file appenders.
     *
     * Note that buffering has to be enabled in
     * {@link #initLog4jHelper(boolean)} for this to have any effect.
     */
    private static final int BUFFER_SIZE = 8192;

    /** Log4j LoggerContext. */
    private static LoggerContext context;
    /** Log4j Configuration. */
    private static Configuration config;
    /** Serializable Log4j Root Logger Configuration. */
    private static LoggerConfig rootLoggerConfig;
    private static Layout<String> layout;
    private static boolean useBuffer;
    private static boolean initialized = false;

    /**
     * @param bufferLogging
     *            Whether output to logfile should be buffered. Enabling
     *            buffering increases performance, as less locks will occur.
     *            However one will not see log messages directly in the log
     *            file.
     */
    public static void initLog4jHelper(boolean bufferLogging) {
        context = (LoggerContext) LogManager.getContext(false);
        config = context.getConfiguration();
        rootLoggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        layout = PatternLayout.createLayout(LOGGING_PATTERN, config, null,
            Constants.CHARSET, true, true, null, null);

        useBuffer = bufferLogging;
        initialized = true;
    }

    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Call #initLoggingHelper() before "
                + "using any other methods of this class.");
        }
    }

    public static Level getLogLevel() {
        checkInitialized();

        return fromLog4jLevel(rootLoggerConfig.getLevel());
    }

    public static void setLogLevel(Level level) {
        checkInitialized();
        checkNotNull(level);

        rootLoggerConfig.setLevel(toLog4jLevel(level));
        context.updateLoggers();
    }

    private static Level fromLog4jLevel(org.apache.logging.log4j.Level level) {
        if (level == org.apache.logging.log4j.Level.TRACE) {
            return Level.TRACE;
        } else if (level == org.apache.logging.log4j.Level.DEBUG) {
            return Level.DEBUG;
        } else if (level == org.apache.logging.log4j.Level.INFO) {
            return Level.INFO;
        } else if (level == org.apache.logging.log4j.Level.WARN) {
            return Level.WARN;
        } else if (level == org.apache.logging.log4j.Level.ERROR) {
            return Level.ERROR;
        } else {
            throw new UnsupportedOperationException(String.format(
                "The Glmtk Logging Wrapper only supports the levels: %s",
                join(Level.values(), ", ")));
        }
    }

    private static org.apache.logging.log4j.Level toLog4jLevel(Level level) {
        switch (level) {
            case TRACE:
                return org.apache.logging.log4j.Level.TRACE;
            case DEBUG:
                return org.apache.logging.log4j.Level.DEBUG;
            case INFO:
                return org.apache.logging.log4j.Level.INFO;
            case WARN:
                return org.apache.logging.log4j.Level.WARN;
            case ERROR:
                return org.apache.logging.log4j.Level.ERROR;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public static void addLoggingConsoleAppender(Target target) {
        checkInitialized();
        checkNotNull(target);

        Appender consoleApender = ConsoleAppender.createAppender(layout, null,
            target.toString(), target.toString() + "Log", "true", "false");
        consoleApender.start();
        config.addAppender(consoleApender);
        rootLoggerConfig.addAppender(consoleApender, null, null);

        context.updateLoggers();
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
    public static void addLoggingFileAppender(Path logFile,
                                              String name,
                                              boolean append) {
        checkInitialized();
        checkNotNull(logFile);
        checkNotNull(name);
        checkArgument(!name.isEmpty());

        int bufferSize = useBuffer ? BUFFER_SIZE : 0;

        Appender fileLocalAppender =
            FileAppender.createAppender(logFile.toString(),
                Boolean.toString(append), "false", name, "false", "false",
                Boolean.toString(useBuffer), Integer.toString(bufferSize),
                layout, null, "false", "false", config);
        fileLocalAppender.start();
        config.addAppender(fileLocalAppender);
        rootLoggerConfig.addAppender(fileLocalAppender, null, null);

        context.updateLoggers();
    }
}
