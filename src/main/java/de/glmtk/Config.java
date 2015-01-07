package de.glmtk;

import static de.glmtk.Constants.MiB;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.StringUtils;

/**
 * All field values (except those declared final) are read from config file.
 */
public enum Config {
    CONFIG;

    private static final Logger LOGGER = LogManager.getFormatterLogger(Config.class);

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
            loadConfig(file);
        } catch (Exception e) {
            // Because of enum nature it is necessary to not throw any checked
            // exceptions during construction.
            System.err.println(e.getMessage());
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

    private void loadConfig(Path file) throws IOException {
        Map<String, Field> fields = new HashMap<>();
        for (Field field : Config.class.getDeclaredFields()) {
            if (isNotConfigurableField(field))
                continue;
            fields.put(field.getName(), field);
        }

        try (BufferedReader reader = Files.newBufferedReader(file,
                Constants.CHARSET)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                if (line.trim().isEmpty() || line.charAt(0) == '#')
                    continue;

                List<String> keyValue = StringUtils.splitAtChar(line, '=');
                if (keyValue.size() != 2)
                    throw new FileFormatException(line, lineNo, file, "config",
                            null,
                            "Entrys have to be in the form of '<key> = <value>'.");

                String key = keyValue.get(0).trim();
                String value = keyValue.get(1).trim();

                if (!fields.containsKey(key))
                    throw new FileFormatException(line, lineNo, file, "config",
                            "key", "Unknown key '%s'.", key);
                Field field = fields.get(key);
                if (field == null)
                    throw new FileFormatException(line, lineNo, file, "config",
                            "key", "Duplicated key '%s'.", key);

                try {
                    Class<?> type = field.getType();
                    if (type.equals(int.class)
                            || field.getType().equals(Integer.class))
                        try {
                            field.set(this, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            throw new FileFormatException(line, lineNo, file,
                                    "config", "value",
                                    "Expected number, found '%s'.", value);
                        }
                    else if (type.equals(long.class))
                        try {
                            field.set(this, Long.parseLong(value));
                        } catch (NumberFormatException e) {
                            throw new FileFormatException(line, lineNo, file,
                                    "config", "value",
                                    "Expected number, found '%s'.", value);
                        }
                    else if (type.equals(Path.class)) {
                        Path path = Paths.get(value);
                        if (path == null)
                            throw new FileFormatException(line, lineNo, file,
                                    "config", "value",
                                    "Expected path, found '%s'.", value);
                        field.set(this, path);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // Shouldn't be possible
                    throw new IllegalStateException(e);
                }

                switch (field.getName()) {
                    case "mainMemory":
                        mainMemory *= MiB;
                        break;

                    case "readerMemory":
                        if (readerMemory * MiB > Integer.MAX_VALUE)
                            throw new FileFormatException(
                                    line,
                                    lineNo,
                                    file,
                                    "config",
                                    "readerMemory value",
                                    "To large readerMemory `%d` specified in '%s'. Does not fit into integer.",
                                    humanReadableByteCount(readerMemory * MiB));
                        readerMemory *= MiB;
                        break;

                    case "writerMemory":
                        if (writerMemory * MiB > Integer.MAX_VALUE)
                            throw new FileFormatException(
                                    line,
                                    lineNo,
                                    file,
                                    "config",
                                    "writerMemory value",
                                    "To large writerMemory `%d` specified in '%s'. Does not fit into integer.",
                                    humanReadableByteCount(writerMemory * MiB));
                        writerMemory *= MiB;
                        break;

                    case "maxChunkSize":
                        maxChunkSize *= MiB;
                        break;

                    case "trainingCacheThreshold":
                        trainingCacheThreshold *= MiB;
                        break;

                    default:
                }

                fields.put(key, null);
            }
        }

        for (Entry<String, Field> entry : fields.entrySet())
            if (entry.getValue() != null)
                throw new FileFormatException(file, "config",
                        "Missing key '%s'.", entry.getKey());
    }

    private boolean isNotConfigurableField(Field field) {
        int mod = field.getModifiers();
        return Modifier.isStatic(mod) || Modifier.isFinal(mod)
                || field.isEnumConstant();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Field field : Config.class.getDeclaredFields()) {
            if (isNotConfigurableField(field))
                continue;

            Object value = null;
            try {
                value = field.get(this);
            } catch (IllegalAccessException e) {
                // Shouldn't be possible
                throw new RuntimeException(e);
            }

            result.append(field.getName()).append('=').append(value).append(
                    "; ");
        }
        return result.toString();
    }
}
