package de.glmtk.common;

import static de.glmtk.Constants.MiB;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.ScalarEvent;

import de.glmtk.Constants;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.util.AbstractYamlParser;
import de.glmtk.util.StringUtils;

/**
 * All field values (except those declared final) are read from config file.
 */
public enum Config {
    CONFIG;

    private static final Logger LOGGER = LogManager.getFormatterLogger(Config.class);

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

    /**
     * The directory the user started the program from.
     */
    private final Path userDir;

    /**
     * The directory where the GLMTK bundle resides (e.g. directory where config
     * file is).
     */
    private final Path glmtkDir;

    /**
     * The directory where log files are saved.
     */
    private final Path logDir;

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

    private Config() {
        userDir = Paths.get(System.getProperty("user.dir"));
        glmtkDir = Paths.get(System.getProperty("glmtk.dir", userDir.toString()));
        logDir = glmtkDir.resolve(Constants.LOG_DIR_NAME);

        Path file = glmtkDir.resolve(Constants.CONFIG_LOCATION);

        try {
            readConfigFromFile(file);
        } catch (Exception e) {
            // Because of enum nature it is necessary to not throw any checked
            // exceptions during construction.
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Path getUserDir() {
        return userDir;
    }

    public Path getGlmtkDir() {
        return glmtkDir;
    }

    public Path getLogDir() {
        return logDir;
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

    public long getTrainingCacheThreshold() {
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
        LOGGER.info("Config %s", StringUtils.repeat("-",
                80 - "Config ".length()));
        //@formatter:off
        LOGGER.info("userDir:                     %s",   userDir);
        LOGGER.info("glmtkDir:                    %s",   glmtkDir);
        LOGGER.info("logDir:                      %s",   logDir);
        LOGGER.info(StringUtils.repeat("-", 80));
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
        //        LOGGER.debug("Reading config from file '%s'.", file);

        new ConfigParser(file).run();
    }
}
