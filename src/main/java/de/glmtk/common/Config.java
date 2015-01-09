package de.glmtk.common;

import static de.glmtk.Constants.MiB;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.ScalarEvent;

import de.glmtk.GlmtkPaths;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.util.AbstractYamlParser;
import de.glmtk.util.StringUtils;

/**
 * All field values (except those declared final) are read from config file.
 */
public class Config {
    private static final Logger LOGGER = LogManager.getFormatterLogger(Config.class);

    private class configParser extends AbstractYamlParser {
        public configParser(Path file) {
            super(file, "config");
        }

        @Override
        protected void parse() {
            parseBegining("!config");
            parseconfig();
            parseEnding();
        }

        protected void parseconfig() {
            Map<String, Boolean> keys = createValidKeysMap("numberOfThreads",
                    "memory", "updateInterval", "tagging");

            event = iter.next();
            while (!event.is(ID.MappingEnd)) {
                assertEventIsId(ID.Scalar);

                String key = ((ScalarEvent) event).getValue();
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
        readconfigFromFile(GlmtkPaths.config_FILE);
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

    public void logconfig() {
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

    private void readconfigFromFile(Path file) throws IOException {
        LOGGER.debug("Reading config from file '%s'.", file);

        new configParser(file).run();
    }
}
