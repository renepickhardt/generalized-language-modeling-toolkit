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
                        mainMemory = parseLongMiB();
                        break;

                    case "reader":
                        readerMemory = parseIntMiB();
                        break;

                    case "writer":
                        writerMemory = parseIntMiB();
                        break;

                    case "chunkSize":
                        maxChunkSize = parseLongMiB();
                        break;

                    case "cacheThreshold":
                        trainingCacheThreshold = parseLongMiB();
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
            long result = parseLong();
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
                        logUpdateInterval = parseInt();
                        break;

                    case "console":
                        consoleUpdateInterval = parseInt();
                        break;

                    case "consoleParams":
                        consoleParamsUpdateInterval = parseInt();
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
                        model = parsePath();
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

    private long mainMemory;
    private int numberOfThreads;
    private int readerMemory;
    private int writerMemory;
    private long maxChunkSize;
    private long trainingCacheThreshold;
    private int consoleUpdateInterval;
    private int logUpdateInterval;
    private int consoleParamsUpdateInterval;
    private Path model;

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

    public long getMainMemory() {
        return mainMemory;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public int getReaderMemory() {
        return readerMemory;
    }

    public int getWriterMemory() {
        return writerMemory;
    }

    public long getMaxChunkSize() {
        return maxChunkSize;
    }

    public long getTrainingCacheThreshold() {
        return trainingCacheThreshold;
    }

    public int getConsoleUpdateInterval() {
        return consoleUpdateInterval;
    }

    public int getLogUpdateInterval() {
        return logUpdateInterval;
    }

    public int getConsoleParamsUpdateInterval() {
        return consoleParamsUpdateInterval;
    }

    public Path getModel() {
        return model;
    }

    public void logConfig() {
        LOGGER.info("Config %s", StringUtils.repeat("-",
                80 - "Config ".length()));
        LOGGER.info("userDir:                     %s", userDir);
        LOGGER.info("glmtkDir:                    %s", glmtkDir);
        LOGGER.info("logDir:                      %s", logDir);
        LOGGER.info(StringUtils.repeat("-", 80));
        LOGGER.info("mainMemory:                  %s",
                humanReadableByteCount(mainMemory));
        LOGGER.info("numberOfThreads:             %d", numberOfThreads);
        LOGGER.info("readerMemory:                %s",
                humanReadableByteCount(readerMemory));
        LOGGER.info("writerMemory:                %s",
                humanReadableByteCount(writerMemory));
        LOGGER.info("maxChunkSize:                %s",
                humanReadableByteCount(maxChunkSize));
        LOGGER.info("trainingCacheThreshold:      %s",
                humanReadableByteCount(trainingCacheThreshold));
        LOGGER.info("consoleUpdateInterval:       %dms", consoleUpdateInterval);
        LOGGER.info("logUpdateInterval:           %dms", logUpdateInterval);
        LOGGER.info("consoleParamsUpdateInterval: %dms",
                consoleParamsUpdateInterval);
        LOGGER.info("model:                       %s", model);
    }

    private void readConfigFromFile(Path file) throws IOException {
        //        LOGGER.debug("Reading config from file '%s'.", file);

        new ConfigParser(file).run();
    }
}
