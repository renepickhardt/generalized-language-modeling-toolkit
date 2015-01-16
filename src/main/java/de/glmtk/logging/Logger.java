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

import de.glmtk.exceptions.SwitchCaseNotImplementedException;

/**
 * Wrapper around {@link org.slf4j.Logger} to include formatting with
 * {@link Formatter} style instead of SLF4J style.
 *
 * <p>
 * Includes some convenience methods.
 */
public class Logger implements org.slf4j.Logger {
    public static Logger get(Class<?> clazz) {
        return new Logger(clazz);
    }

    public static enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR;
    }

    private org.slf4j.Logger logger;
    private StringBuilder stringBuilder;
    private Formatter formatter;

    private Logger(Class<?> clazz) {
        logger = LoggerFactory.getLogger(clazz);
        stringBuilder = new StringBuilder();
        formatter = new Formatter(stringBuilder);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(String format,
                      Object arg) {
        if (isTraceEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.trace(formatter.toString());
        }
    }

    @Override
    public void trace(String format,
                      Object arg1,
                      Object arg2) {
        if (isTraceEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.trace(formatter.toString());
        }
    }

    @Override
    public void trace(String format,
                      Object... arguments) {
        if (isTraceEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.trace(formatter.toString());
        }
    }

    @Override
    public void trace(String msg,
                      Throwable t) {
        logger.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker,
                      String msg) {
        logger.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker,
                      String format,
                      Object arg) {
        if (isTraceEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.trace(marker, formatter.toString());
        }
    }

    @Override
    public void trace(Marker marker,
                      String format,
                      Object arg1,
                      Object arg2) {
        if (isTraceEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.trace(marker, formatter.toString());
        }
    }

    @Override
    public void trace(Marker marker,
                      String format,
                      Object... argArray) {
        if (isTraceEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, argArray);
            logger.trace(marker, formatter.toString());
        }
    }

    @Override
    public void trace(Marker marker,
                      String msg,
                      Throwable t) {
        logger.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format,
                      Object arg) {
        if (isDebugEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.debug(formatter.toString());
        }
    }

    @Override
    public void debug(String format,
                      Object arg1,
                      Object arg2) {
        if (isDebugEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.debug(formatter.toString());
        }
    }

    @Override
    public void debug(String format,
                      Object... arguments) {
        if (isDebugEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.debug(formatter.toString());
        }
    }

    @Override
    public void debug(String msg,
                      Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker,
                      String msg) {
        logger.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker,
                      String format,
                      Object arg) {
        if (isDebugEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.debug(marker, formatter.toString());
        }
    }

    @Override
    public void debug(Marker marker,
                      String format,
                      Object arg1,
                      Object arg2) {
        if (isDebugEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.debug(marker, formatter.toString());
        }
    }

    @Override
    public void debug(Marker marker,
                      String format,
                      Object... arguments) {
        if (isDebugEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.debug(marker, formatter.toString());
        }
    }

    @Override
    public void debug(Marker marker,
                      String msg,
                      Throwable t) {
        logger.debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String format,
                     Object arg) {
        if (isInfoEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.info(formatter.toString());
        }
    }

    @Override
    public void info(String format,
                     Object arg1,
                     Object arg2) {
        if (isInfoEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.info(formatter.toString());
        }
    }

    @Override
    public void info(String format,
                     Object... arguments) {
        if (isInfoEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.info(formatter.toString());
        }
    }

    @Override
    public void info(String msg,
                     Throwable t) {
        logger.info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker,
                     String msg) {
        logger.info(marker, msg);
    }

    @Override
    public void info(Marker marker,
                     String format,
                     Object arg) {
        if (isInfoEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.info(marker, formatter.toString());
        }
    }

    @Override
    public void info(Marker marker,
                     String format,
                     Object arg1,
                     Object arg2) {
        if (isInfoEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.info(marker, formatter.toString());
        }
    }

    @Override
    public void info(Marker marker,
                     String format,
                     Object... arguments) {
        if (isInfoEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.info(marker, formatter.toString());
        }
    }

    @Override
    public void info(Marker marker,
                     String msg,
                     Throwable t) {
        logger.info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String format,
                     Object arg) {
        if (isWarnEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.warn(formatter.toString());
        }
    }

    @Override
    public void warn(String format,
                     Object... arguments) {
        if (isWarnEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.warn(formatter.toString());
        }
    }

    @Override
    public void warn(String format,
                     Object arg1,
                     Object arg2) {
        if (isWarnEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.warn(formatter.toString());
        }
    }

    @Override
    public void warn(String msg,
                     Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker,
                     String msg) {
        logger.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker,
                     String format,
                     Object arg) {
        if (isWarnEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.warn(marker, formatter.toString());
        }
    }

    @Override
    public void warn(Marker marker,
                     String format,
                     Object arg1,
                     Object arg2) {
        if (isWarnEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.warn(marker, formatter.toString());
        }
    }

    @Override
    public void warn(Marker marker,
                     String format,
                     Object... arguments) {
        if (isWarnEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.warn(marker, formatter.toString());
        }
    }

    @Override
    public void warn(Marker marker,
                     String msg,
                     Throwable t) {
        logger.warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String format,
                      Object arg) {
        if (isErrorEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.error(formatter.toString());
        }
    }

    @Override
    public void error(String format,
                      Object arg1,
                      Object arg2) {
        if (isErrorEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.error(formatter.toString());
        }
    }

    @Override
    public void error(String format,
                      Object... arguments) {
        if (isErrorEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.error(formatter.toString());
        }
    }

    @Override
    public void error(String msg,
                      Throwable t) {
        logger.error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker,
                      String msg) {
        logger.error(marker, msg);
    }

    @Override
    public void error(Marker marker,
                      String format,
                      Object arg) {
        if (isErrorEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg);
            logger.error(marker, formatter.toString());
        }
    }

    @Override
    public void error(Marker marker,
                      String format,
                      Object arg1,
                      Object arg2) {
        if (isErrorEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arg1, arg2);
            logger.error(marker, formatter.toString());
        }
    }

    @Override
    public void error(Marker marker,
                      String format,
                      Object... arguments) {
        if (isErrorEnabled()) {
            stringBuilder.setLength(0);
            formatter.format(format, arguments);
            logger.error(marker, formatter.toString());
        }
    }

    @Override
    public void error(Marker marker,
                      String msg,
                      Throwable t) {
        logger.error(marker, msg, t);
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

    public void log(Level level,
                    String msg) {
        switch (level) {
            case TRACE:
                trace(msg);
                break;
            case DEBUG:
                debug(msg);
                break;
            case INFO:
                info(msg);
                break;
            case WARN:
                warn(msg);
                break;
            case ERROR:
                error(msg);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    String format,
                    Object arg) {
        switch (level) {
            case TRACE:
                trace(format, arg);
                break;
            case DEBUG:
                debug(format, arg);
                break;
            case INFO:
                info(format, arg);
                break;
            case WARN:
                warn(format, arg);
                break;
            case ERROR:
                error(format, arg);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    String format,
                    Object arg1,
                    Object arg2) {
        switch (level) {
            case TRACE:
                trace(format, arg1, arg2);
                break;
            case DEBUG:
                debug(format, arg1, arg2);
                break;
            case INFO:
                info(format, arg1, arg2);
                break;
            case WARN:
                warn(format, arg1, arg2);
                break;
            case ERROR:
                error(format, arg1, arg2);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    String format,
                    Object... arguments) {
        switch (level) {
            case TRACE:
                trace(format, arguments);
                break;
            case DEBUG:
                debug(format, arguments);
                break;
            case INFO:
                info(format, arguments);
                break;
            case WARN:
                warn(format, arguments);
                break;
            case ERROR:
                error(format, arguments);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    String msg,
                    Throwable t) {
        switch (level) {
            case TRACE:
                trace(msg, t);
                break;
            case DEBUG:
                debug(msg, t);
                break;
            case INFO:
                info(msg, t);
                break;
            case WARN:
                warn(msg, t);
                break;
            case ERROR:
                error(msg, t);
                break;
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

    public void log(Level level,
                    Marker marker,
                    String msg) {
        switch (level) {
            case TRACE:
                trace(marker, msg);
                break;
            case DEBUG:
                debug(marker, msg);
                break;
            case INFO:
                info(marker, msg);
                break;
            case WARN:
                warn(marker, msg);
                break;
            case ERROR:
                error(marker, msg);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    Marker marker,
                    String format,
                    Object arg) {
        switch (level) {
            case TRACE:
                trace(marker, format, arg);
                break;
            case DEBUG:
                debug(marker, format, arg);
                break;
            case INFO:
                info(marker, format, arg);
                break;
            case WARN:
                warn(marker, format, arg);
                break;
            case ERROR:
                error(marker, format, arg);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    Marker marker,
                    String format,
                    Object arg1,
                    Object arg2) {
        switch (level) {
            case TRACE:
                trace(marker, format, arg1, arg2);
                break;
            case DEBUG:
                debug(marker, format, arg1, arg2);
                break;
            case INFO:
                info(marker, format, arg1, arg2);
                break;
            case WARN:
                warn(marker, format, arg1, arg2);
                break;
            case ERROR:
                error(marker, format, arg1, arg2);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    Marker marker,
                    String format,
                    Object... argArray) {
        switch (level) {
            case TRACE:
                trace(marker, format, argArray);
                break;
            case DEBUG:
                debug(marker, format, argArray);
                break;
            case INFO:
                info(marker, format, argArray);
                break;
            case WARN:
                warn(marker, format, argArray);
                break;
            case ERROR:
                error(marker, format, argArray);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public void log(Level level,
                    Marker marker,
                    String msg,
                    Throwable t) {
        switch (level) {
            case TRACE:
                trace(marker, msg, t);
                break;
            case DEBUG:
                debug(marker, msg, t);
                break;
            case INFO:
                info(marker, msg, t);
                break;
            case WARN:
                warn(marker, msg, t);
                break;
            case ERROR:
                error(marker, msg, t);
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }
}
