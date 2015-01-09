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

    private static final String loggingPattern = "%date{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger - %msg%n";

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
