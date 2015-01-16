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

package de.glmtk.common;

import static de.glmtk.Constants.MiB;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.ScalarEvent;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.logging.Logger;
import de.glmtk.util.AbstractYamlParser;
import de.glmtk.util.StringUtils;

/**
 * All field values (except those declared final) are read from config file.
 */
public class Config {
    private static final Logger LOGGER = Logger.get(Config.class);

    private class ConfigParser extends AbstractYamlParser {
        public ConfigParser(Path file) {
            super(file, "config");
        }

        @Override
        protected void parse() {
            parseBegining("!config");
            parseConfig();
            parseEnding();
        }

        protected void parseConfig() {
            Map<String, Boolean> keys = createValidKeysMap("numberOfThreads",
                    "memory", "updateInterval", "tagging");

            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                String key = parseScalar();
                registerKey(keys, key);

                event = iter.next();
                switch (key) {
                    case "numberOfThreads":
                        numberOfThreads = parseInt();
                        break;

                    case "memory":
                        parseMemory();
                        break;

                    case "updateInterval":
                        parseUpdateInterval();
                        break;

                    case "tagging":
                        parseTagging();
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                event = iter.next();
            }
        }

        private void parseMemory() {
            assertEventIsId(ID.MappingStart);

            Map<String, Boolean> keys = createValidKeysMap("jvm", "reader",
                    "writer", "chunkSize", "cacheThreshold");

            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                assertEventIsId(ID.Scalar);

                String key = ((ScalarEvent) event).getValue();
                registerKey(keys, key);

                event = iter.next();
                switch (key) {
                    case "jvm":
                        memoryJvm = parseLongMiB();
                        break;

                    case "reader":
                        memoryReader = parseIntMiB();
                        break;

                    case "writer":
                        memoryWriter = parseIntMiB();
                        break;

                    case "chunkSize":
                        memoryChunkSize = parseLongMiB();
                        break;

                    case "cacheThreshold":
                        memoryCacheThreshold = parseLongMiB();
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                event = iter.next();
            }
        }

        private long parseLongMiB() {
            return parseLong() * MiB;
        }

        private int parseIntMiB() {
            long result = parseLongMiB();
            if (result >= Integer.MAX_VALUE)
                throw newFileFormatException(
                        "Given memory value is to large for integer: %s resp. %d bytes.",
                        humanReadableByteCount(result), result);
            return (int) result;
        }

        private void parseUpdateInterval() {
            assertEventIsId(ID.MappingStart);

            Map<String, Boolean> keys = createValidKeysMap("log", "console",
                    "consoleParams");

            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                assertEventIsId(ID.Scalar);

                String key = ((ScalarEvent) event).getValue();
                registerKey(keys, key);

                event = iter.next();
                switch (key) {
                    case "log":
                        updateIntervalLog = parseInt();
                        break;

                    case "console":
                        updateIntervalConsole = parseInt();
                        break;

                    case "consoleParams":
                        updateIntervalConsoleParams = parseInt();
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                event = iter.next();
            }
        }

        private void parseTagging() {
            assertEventIsId(ID.MappingStart);

            Map<String, Boolean> keys = createValidKeysMap("model");

            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                assertEventIsId(ID.Scalar);

                String key = ((ScalarEvent) event).getValue();
                registerKey(keys, key);

                event = iter.next();
                switch (key) {
                    case "model":
                        taggingModel = parsePath();
                        break;

                    default:
                        throw new SwitchCaseNotImplementedException();
                }

                event = iter.next();
            }
        }
    }

    private int numberOfThreads;
    private long memoryJvm;
    private int memoryReader;
    private int memoryWriter;
    private long memoryChunkSize;
    private long memoryCacheThreshold;
    private int updateIntervalLog;
    private int updateIntervalConsole;
    private int updateIntervalConsoleParams;
    private Path taggingModel;

    public Config() throws IOException {
        readConfigFromFile(GlmtkPaths.CONFIG_FILE);
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public long getMemoryJvm() {
        return memoryJvm;
    }

    public int getMemoryReader() {
        return memoryReader;
    }

    public int getMemoryWriter() {
        return memoryWriter;
    }

    public long getMemoryChunkSize() {
        return memoryChunkSize;
    }

    public long getMemoryCacheThreshold() {
        return memoryCacheThreshold;
    }

    public int getUpdateIntervalLog() {
        return updateIntervalLog;
    }

    public int getUpdateIntervalConsole() {
        return updateIntervalConsole;
    }

    public int getUpdateIntervalConsoleParams() {
        return updateIntervalConsoleParams;
    }

    public Path getTaggingModel() {
        return taggingModel;
    }

    public void logConfig() {
        LOGGER.info("config %s", StringUtils.repeat("-",
                80 - "config ".length()));
        //@formatter:off
        LOGGER.info("numberOfThreads:             %d",   numberOfThreads);
        LOGGER.info("memoryJvm:                   %s",   humanReadableByteCount(memoryJvm));
        LOGGER.info("memoryReader:                %s",   humanReadableByteCount(memoryReader));
        LOGGER.info("memoryWriter:                %s",   humanReadableByteCount(memoryWriter));
        LOGGER.info("memoryChunkSize:             %s",   humanReadableByteCount(memoryChunkSize));
        LOGGER.info("memoryCacheThreshold:        %s",   humanReadableByteCount(memoryCacheThreshold));
        LOGGER.info("updateIntervalConsole:       %dms", updateIntervalConsole);
        LOGGER.info("updateIntervalLog:           %dms", updateIntervalLog);
        LOGGER.info("updateIntervalConsoleParams: %dms", updateIntervalConsoleParams);
        LOGGER.info("taggingModel:                %s",   taggingModel);
        //@formatter:on
    }

    private void readConfigFromFile(Path file) throws IOException {
        LOGGER.debug("Reading config from file '%s'.", file);

        try {
            new ConfigParser(file).run();
        } catch (NoSuchFileException e) {
            throw new NoSuchFileException(
                    String.format(
                            "Config file missing: Could not open '%s'.%n"
                                    + "Did you copy '%s.sample' to '%s' in the installation directory '%s'?",
                            file, Constants.CONFIG_FILE, Constants.CONFIG_FILE,
                            GlmtkPaths.GLMTK_DIR));
        }
    }
}
