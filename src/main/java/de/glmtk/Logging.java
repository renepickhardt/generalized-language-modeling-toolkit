package de.glmtk;

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
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class Logging {

    private static final String loggingPattern =
            "%date{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger - %msg%n";

    private static final String loggingHighlightPattern =
            "%highlight{"
                    + loggingPattern
                    + "}{FATAL=red, ERROR=red, WARN=yellow, INFO=normal, DEBUG=green, TRACE=blue}";

    /**
     * GLMTK own configuration.
     */
    private static Config config = Config.get();

    /**
     * Log4j LoggerContext.
     */
    private static LoggerContext loggerContext = (LoggerContext) LogManager
            .getContext(false);

    /**
     * Log4j Configuration.
     */
    private static Configuration configuration = loggerContext
            .getConfiguration();

    /**
     * Log4j Root Logger Configuration.
     */
    private static LoggerConfig loggerConfig = configuration
            .getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

    public static Level getLogLevel() {
        return loggerConfig.getLevel();
    }

    public static void configureExecLogging() {
        Layout<String> layout =
                PatternLayout.createLayout(loggingPattern, configuration, null,
                        Charset.defaultCharset(), true, true, null, null);
        Layout<String> layoutHighlight =
                PatternLayout.createLayout(loggingHighlightPattern,
                        configuration, null, Charset.defaultCharset(), true,
                        true, null, null);

        Appender consoleApender =
                ConsoleAppender.createAppender(layoutHighlight, null,
                        "SYSTEM_OUT", "Console", "true", "false");
        consoleApender.start();

        Appender fileAllAppender =
                FileAppender.createAppender(
                        config.getLogDir().resolve("all.log").toString(),
                        "true", "false", "FileAll", "false", "false", "true",
                        "8192", layout, null, "false", "false", configuration);
        fileAllAppender.start();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = format.format(Calendar.getInstance().getTime());
        Appender fileOwnAppender =
                FileAppender.createAppender(config.getLogDir().resolve(time)
                        .toString(), "false", "false", "FileOwn", "false",
                        "false", "true", "8192", layout, null, "false",
                        "false", configuration);
        fileOwnAppender.start();

        configuration.addAppender(consoleApender);
        configuration.addAppender(fileAllAppender);
        configuration.addAppender(fileOwnAppender);

        loggerConfig.addAppender(consoleApender, null, null);
        loggerConfig.addAppender(fileAllAppender, null, null);
        loggerConfig.addAppender(fileOwnAppender, null, null);

        loggerContext.updateLoggers();
    }

    public static void configureTestLogging() {
        Layout<String> layout =
                PatternLayout.createLayout(loggingPattern, configuration, null,
                        Charset.defaultCharset(), true, true, null, null);

        Appender consoleApender =
                ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT",
                        "Console", "true", "false");
        consoleApender.start();
        configuration.addAppender(consoleApender);
        loggerConfig.addAppender(consoleApender, null, null);

        loggerContext.updateLoggers();
    }

    public static void addLocalFileAppender(Path localLogFile) {
        Layout<String> layout =
                PatternLayout.createLayout(loggingPattern, configuration, null,
                        Charset.defaultCharset(), true, true, null, null);

        Appender fileLocalAppender =
                FileAppender.createAppender(localLogFile.toString(), "true",
                        "false", "FileLocal", "false", "false", "true", "8192",
                        layout, null, "false", "false", configuration);
        fileLocalAppender.start();
        configuration.addAppender(fileLocalAppender);
        loggerConfig.addAppender(fileLocalAppender, null, null);

        loggerContext.updateLoggers();
    }

}
