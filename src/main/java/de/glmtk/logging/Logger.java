/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2015 Lukas Schmelzeisen
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

import java.util.Formatter;

import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import de.glmtk.exceptions.SwitchCaseNotImplementedException;

/**
 * Wrapper around {@link org.slf4j.Logger} to include formatting with
 * {@link Formatter} style instead of SLF4J style.
 *
 * <p>
 * Includes some convenience methods.
 */
public class Logger implements org.slf4j.Logger {
    private static String FCQN = Logger.class.getName();

    private static boolean traceEnabled = true;
    private static boolean debugEnabled = true;
    private static boolean infoEnabled = true;
    private static boolean warnEnabled = true;
    private static boolean errorEnabled = true;

    public static enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR;

        public int toInt() {
            switch (this) {
                case TRACE:
                    return LocationAwareLogger.TRACE_INT;
                case DEBUG:
                    return LocationAwareLogger.DEBUG_INT;
                case INFO:
                    return LocationAwareLogger.INFO_INT;
                case WARN:
                    return LocationAwareLogger.WARN_INT;
                case ERROR:
                    return LocationAwareLogger.ERROR_INT;
                default:
                    throw new SwitchCaseNotImplementedException();
            }
        }

        public boolean isMoreSpecificThan(Level level) {
            return toInt() >= level.toInt();
        }

        public boolean isLessSpecificThan(Level level) {
            return toInt() <= level.toInt();
        }
    }

    public static Logger get(Class<?> clazz) {
        return new Logger(clazz);
    }

    public static void setTraceEnabled(boolean traceEnabled) {
        Logger.traceEnabled = traceEnabled;
    }

    public static void setDebugEnabled(boolean debugEnabled) {
        Logger.debugEnabled = debugEnabled;
    }

    public static void setInfoEnabled(boolean infoEnabled) {
        Logger.infoEnabled = infoEnabled;
    }

    public static void setWarnEnabled(boolean warnEnabled) {
        Logger.warnEnabled = warnEnabled;
    }

    public static void setErrorEnabled(boolean errorEnabled) {
        Logger.errorEnabled = errorEnabled;
    }

    public static void setLoggingEnabled(Level level,
                                         boolean levelEnabled) {
        switch (level) {
            case TRACE:
                setTraceEnabled(levelEnabled);
                break;
            case DEBUG:
                setDebugEnabled(levelEnabled);
                break;
            case INFO:
                setInfoEnabled(levelEnabled);
                break;
            case WARN:
                setWarnEnabled(levelEnabled);
                break;
            case ERROR:
                setErrorEnabled(levelEnabled);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    private org.slf4j.Logger logger;
    private LocationAwareLogger locationAwareLogger;
    private StringBuilder stringBuilder;
    private Formatter formatter;

    private Logger(Class<?> clazz) {
        logger = LoggerFactory.getLogger(clazz);
        locationAwareLogger = null;
        if (logger instanceof LocationAwareLogger)
            locationAwareLogger = (LocationAwareLogger) logger;
        stringBuilder = new StringBuilder();
        formatter = new Formatter(stringBuilder);

        traceEnabled = true;
        debugEnabled = true;
        infoEnabled = true;
        warnEnabled = true;
        errorEnabled = true;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return traceEnabled && logger.isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return traceEnabled && logger.isTraceEnabled(marker);
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled && logger.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return debugEnabled && logger.isDebugEnabled(marker);
    }

    @Override
    public boolean isInfoEnabled() {
        return infoEnabled && logger.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return infoEnabled && logger.isInfoEnabled(marker);
    }

    @Override
    public boolean isWarnEnabled() {
        return warnEnabled && logger.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return warnEnabled && logger.isWarnEnabled(marker);
    }

    @Override
    public boolean isErrorEnabled() {
        return errorEnabled && logger.isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return errorEnabled && logger.isErrorEnabled(marker);
    }

    public boolean isLoggingEnabled(Level level) {
        switch (level) {
            case TRACE:
                return isTraceEnabled();
            case DEBUG:
                return isDebugEnabled();
            case INFO:
                return isInfoEnabled();
            case WARN:
                return isWarnEnabled();
            case ERROR:
                return isErrorEnabled();
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public boolean isLoggingEnabled(Level level,
                                    Marker marker) {
        switch (level) {
            case TRACE:
                return isTraceEnabled(marker);
            case DEBUG:
                return isDebugEnabled(marker);
            case INFO:
                return isInfoEnabled(marker);
            case WARN:
                return isWarnEnabled(marker);
            case ERROR:
                return isErrorEnabled(marker);
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled())
            log(Level.TRACE, (Marker) null, msg, (Throwable) null);
    }

    @Override
    public void trace(String format,
                      Object arg) {
        if (isTraceEnabled())
            log(Level.TRACE, (Marker) null, makeMessage(format, arg),
                    (Throwable) null);
    }

    @Override
    public void trace(String format,
                      Object arg1,
                      Object arg2) {
        if (isTraceEnabled())
            log(Level.TRACE, (Marker) null, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void trace(String format,
                      Object... args) {
        if (isTraceEnabled())
            log(Level.TRACE, (Marker) null, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void trace(String msg,
                      Throwable t) {
        if (isTraceEnabled())
            log(Level.TRACE, (Marker) null, msg, t);
    }

    @Override
    public void trace(Marker marker,
                      String msg) {
        if (isTraceEnabled(marker))
            log(Level.TRACE, marker, msg, (Throwable) null);
    }

    @Override
    public void trace(Marker marker,
                      String format,
                      Object arg) {
        if (isTraceEnabled(marker))
            log(Level.TRACE, marker, makeMessage(format, arg), (Throwable) null);
    }

    @Override
    public void trace(Marker marker,
                      String format,
                      Object arg1,
                      Object arg2) {
        if (isTraceEnabled(marker))
            log(Level.TRACE, marker, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void trace(Marker marker,
                      String format,
                      Object... args) {
        if (isTraceEnabled(marker))
            log(Level.TRACE, marker, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void trace(Marker marker,
                      String msg,
                      Throwable t) {
        if (isTraceEnabled(marker))
            log(Level.TRACE, marker, msg, t);
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled())
            log(Level.DEBUG, (Marker) null, msg, (Throwable) null);
    }

    @Override
    public void debug(String format,
                      Object arg) {
        if (isDebugEnabled())
            log(Level.DEBUG, (Marker) null, makeMessage(format, arg),
                    (Throwable) null);
    }

    @Override
    public void debug(String format,
                      Object arg1,
                      Object arg2) {
        if (isDebugEnabled())
            log(Level.DEBUG, (Marker) null, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void debug(String format,
                      Object... args) {
        if (isDebugEnabled())
            log(Level.DEBUG, (Marker) null, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void debug(String msg,
                      Throwable t) {
        if (isDebugEnabled())
            log(Level.DEBUG, (Marker) null, msg, t);
    }

    @Override
    public void debug(Marker marker,
                      String msg) {
        if (isDebugEnabled(marker))
            log(Level.DEBUG, marker, msg, (Throwable) null);
    }

    @Override
    public void debug(Marker marker,
                      String format,
                      Object arg) {
        if (isDebugEnabled(marker))
            log(Level.DEBUG, marker, makeMessage(format, arg), (Throwable) null);
    }

    @Override
    public void debug(Marker marker,
                      String format,
                      Object arg1,
                      Object arg2) {
        if (isDebugEnabled(marker))
            log(Level.DEBUG, marker, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void debug(Marker marker,
                      String format,
                      Object... args) {
        if (isDebugEnabled(marker))
            log(Level.DEBUG, marker, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void debug(Marker marker,
                      String msg,
                      Throwable t) {
        if (isDebugEnabled(marker))
            log(Level.DEBUG, marker, msg, t);
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled())
            log(Level.INFO, (Marker) null, msg, (Throwable) null);
    }

    @Override
    public void info(String format,
                     Object arg) {
        if (isInfoEnabled())
            log(Level.INFO, (Marker) null, makeMessage(format, arg),
                    (Throwable) null);
    }

    @Override
    public void info(String format,
                     Object arg1,
                     Object arg2) {
        if (isInfoEnabled())
            log(Level.INFO, (Marker) null, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void info(String format,
                     Object... args) {
        if (isInfoEnabled())
            log(Level.INFO, (Marker) null, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void info(String msg,
                     Throwable t) {
        if (isInfoEnabled())
            log(Level.INFO, (Marker) null, msg, t);
    }

    @Override
    public void info(Marker marker,
                     String msg) {
        if (isInfoEnabled(marker))
            log(Level.INFO, marker, msg, (Throwable) null);
    }

    @Override
    public void info(Marker marker,
                     String format,
                     Object arg) {
        if (isInfoEnabled(marker))
            log(Level.INFO, marker, makeMessage(format, arg), (Throwable) null);
    }

    @Override
    public void info(Marker marker,
                     String format,
                     Object arg1,
                     Object arg2) {
        if (isInfoEnabled(marker))
            log(Level.INFO, marker, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void info(Marker marker,
                     String format,
                     Object... args) {
        if (isInfoEnabled(marker))
            log(Level.INFO, marker, makeMessage(format, args), (Throwable) null);
    }

    @Override
    public void info(Marker marker,
                     String msg,
                     Throwable t) {
        if (isInfoEnabled(marker))
            log(Level.INFO, marker, msg, t);
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled())
            log(Level.WARN, (Marker) null, msg, (Throwable) null);
    }

    @Override
    public void warn(String format,
                     Object arg) {
        if (isWarnEnabled())
            log(Level.WARN, (Marker) null, makeMessage(format, arg),
                    (Throwable) null);
    }

    @Override
    public void warn(String format,
                     Object arg1,
                     Object arg2) {
        if (isWarnEnabled())
            log(Level.WARN, (Marker) null, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void warn(String format,
                     Object... args) {
        if (isWarnEnabled())
            log(Level.WARN, (Marker) null, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void warn(String msg,
                     Throwable t) {
        if (isWarnEnabled())
            log(Level.WARN, (Marker) null, msg, t);
    }

    @Override
    public void warn(Marker marker,
                     String msg) {
        if (isWarnEnabled(marker))
            log(Level.WARN, marker, msg, (Throwable) null);
    }

    @Override
    public void warn(Marker marker,
                     String format,
                     Object arg) {
        if (isWarnEnabled(marker))
            log(Level.WARN, marker, makeMessage(format, arg), (Throwable) null);
    }

    @Override
    public void warn(Marker marker,
                     String format,
                     Object arg1,
                     Object arg2) {
        if (isWarnEnabled(marker))
            log(Level.WARN, marker, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void warn(Marker marker,
                     String format,
                     Object... args) {
        if (isWarnEnabled(marker))
            log(Level.WARN, marker, makeMessage(format, args), (Throwable) null);
    }

    @Override
    public void warn(Marker marker,
                     String msg,
                     Throwable t) {
        if (isWarnEnabled(marker))
            log(Level.WARN, marker, msg, t);
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled())
            log(Level.ERROR, (Marker) null, msg, (Throwable) null);
    }

    @Override
    public void error(String format,
                      Object arg) {
        if (isErrorEnabled())
            log(Level.ERROR, (Marker) null, makeMessage(format, arg),
                    (Throwable) null);
    }

    @Override
    public void error(String format,
                      Object arg1,
                      Object arg2) {
        if (isErrorEnabled())
            log(Level.ERROR, (Marker) null, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void error(String format,
                      Object... args) {
        if (isErrorEnabled())
            log(Level.ERROR, (Marker) null, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void error(String msg,
                      Throwable t) {
        if (isErrorEnabled())
            log(Level.ERROR, (Marker) null, msg, t);
    }

    @Override
    public void error(Marker marker,
                      String msg) {
        if (isErrorEnabled(marker))
            log(Level.ERROR, marker, msg, (Throwable) null);
    }

    @Override
    public void error(Marker marker,
                      String format,
                      Object arg) {
        if (isErrorEnabled(marker))
            log(Level.ERROR, marker, makeMessage(format, arg), (Throwable) null);
    }

    @Override
    public void error(Marker marker,
                      String format,
                      Object arg1,
                      Object arg2) {
        if (isErrorEnabled(marker))
            log(Level.ERROR, marker, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    @Override
    public void error(Marker marker,
                      String format,
                      Object... args) {
        if (isErrorEnabled(marker))
            log(Level.ERROR, marker, makeMessage(format, args),
                    (Throwable) null);
    }

    @Override
    public void error(Marker marker,
                      String msg,
                      Throwable t) {
        if (isErrorEnabled(marker))
            log(Level.ERROR, marker, msg, t);
    }

    public void log(Level level,
                    String msg) {
        if (isLoggingEnabled(level))
            log(level, (Marker) null, msg, (Throwable) null);
    }

    public void log(Level level,
                    String format,
                    Object arg) {
        if (isLoggingEnabled(level))
            log(level, (Marker) null, makeMessage(format, arg),
                    (Throwable) null);
    }

    public void log(Level level,
                    String format,
                    Object arg1,
                    Object arg2) {
        if (isLoggingEnabled(level))
            log(level, (Marker) null, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    public void log(Level level,
                    String format,
                    Object... args) {
        if (isLoggingEnabled(level))
            log(level, (Marker) null, makeMessage(format, args),
                    (Throwable) null);
    }

    public void log(Level level,
                    String msg,
                    Throwable t) {
        if (isLoggingEnabled(level))
            log(level, (Marker) null, msg, t);
    }

    public void log(Level level,
                    Marker marker,
                    String msg) {
        if (isLoggingEnabled(level, marker))
            log(level, marker, msg, (Throwable) null);
    }

    public void log(Level level,
                    Marker marker,
                    String format,
                    Object arg) {
        if (isLoggingEnabled(level, marker))
            log(level, marker, makeMessage(format, arg), (Throwable) null);
    }

    public void log(Level level,
                    Marker marker,
                    String format,
                    Object arg1,
                    Object arg2) {
        if (isLoggingEnabled(level, marker))
            log(level, marker, makeMessage(format, arg1, arg2),
                    (Throwable) null);
    }

    public void log(Level level,
                    Marker marker,
                    String format,
                    Object... args) {
        if (isLoggingEnabled(level, marker))
            log(level, marker, makeMessage(format, args), (Throwable) null);
    }

    public void log(Level level,
                    Marker marker,
                    String msg,
                    Throwable t) {
        if (locationAwareLogger != null)
            locationAwareLogger.log(marker, FCQN, level.toInt(), msg, null, t);
        else
            switch (level) {
                case TRACE:
                    logger.trace(marker, msg, t);
                    break;
                case DEBUG:
                    logger.debug(marker, msg, t);
                    break;
                case INFO:
                    logger.info(marker, msg, t);
                    break;
                case WARN:
                    logger.warn(marker, msg, t);
                    break;
                case ERROR:
                    logger.error(marker, msg, t);
                    break;
                default:
                    throw new SwitchCaseNotImplementedException();
            }
    }

    private String makeMessage(String format,
                               Object arg) {
        stringBuilder.setLength(0);
        formatter.format(format, arg);
        return formatter.toString();
    }

    private String makeMessage(String format,
                               Object arg1,
                               Object arg2) {
        stringBuilder.setLength(0);
        formatter.format(format, arg1, arg2);
        return formatter.toString();
    }

    private String makeMessage(String format,
                               Object... args) {
        stringBuilder.setLength(0);
        formatter.format(format, args);
        return formatter.toString();
    }
}
