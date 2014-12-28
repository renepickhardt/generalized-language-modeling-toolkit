package de.glmtk.utils;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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

import de.glmtk.Config;
import de.glmtk.Constants;

public class LogUtils {

    private static final String loggingPattern =
            "%date{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger - %msg%n";

    private static LogUtils instance = null;

    public static LogUtils getInstance() {
        if (instance == null) {
            instance = new LogUtils();
        }
        return instance;
    }

    /**
     * GLMTK own configuration.
     */
    private Config config;

    /**
     * Log4j LoggerContext.
     */
    private LoggerContext loggerContext;

    /**
     * Log4j Configuration.
     */
    private Configuration configuration;

    /**
     * Serializable
     * Log4j Root Logger Configuration.
     */
    private LoggerConfig loggerConfig;

    private Layout<String> layout;

    private LogUtils() {
        config = Config.get();
        loggerContext = (LoggerContext) LogManager.getContext(false);
        configuration = loggerContext.getConfiguration();
        loggerConfig =
                configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        layout =
                PatternLayout.createLayout(loggingPattern, configuration, null,
                        Charset.defaultCharset(), true, true, null, null);
    }

    public Level getLogLevel() {
        return loggerConfig.getLevel();
    }

    public void setUpExecLogging() {
        addFileAppender(
                config.getLogDir().resolve(Constants.ALL_LOG_FILE_NAME),
                "FileAll", true);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(Calendar.getInstance().getTime());
        addFileAppender(config.getLogDir().resolve(time + ".log"),
                "FileTimestamp", false);
    }

    public void setUpTestLogging() {
        addConsoleAppender(Target.SYSTEM_OUT);
    }

    public void addConsoleAppender(Target target) {
        Appender consoleApender =
                ConsoleAppender.createAppender(layout, null, target.toString(),
                        "Console", "true", "false");
        consoleApender.start();
        configuration.addAppender(consoleApender);
        loggerConfig.addAppender(consoleApender, null, null);

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
     *            If {@code true} log messages are appended to the file.
     *            If {@code false} log message replace file contents.
     */
    public void addFileAppender(Path logFile, String name, boolean append) {
        Appender fileLocalAppender =
                FileAppender.createAppender(logFile.toString(),
                        Boolean.toString(append), "false", name, "false",
                        "false", "true", "8192", layout, null, "false",
                        "false", configuration);
        fileLocalAppender.start();
        configuration.addAppender(fileLocalAppender);
        loggerConfig.addAppender(fileLocalAppender, null, null);

        loggerContext.updateLoggers();
    }

}
