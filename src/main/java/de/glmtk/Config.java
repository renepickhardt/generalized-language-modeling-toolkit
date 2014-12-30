package de.glmtk;

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

import de.glmtk.util.StringUtils;

public enum Config {

    CONFIG;

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

    private int mainMemory;

    private int numberOfCores;

    private int consoleUpdateInterval;

    private int logUpdateInterval;

    private Path model;

    public Path getUserDir() {
        return userDir;
    }

    public Path getGlmtkDir() {
        return glmtkDir;
    }

    public Path getLogDir() {
        return logDir;
    }

    public int getMainMemory() {
        return mainMemory;
    }

    public int getNumberOfCores() {
        return numberOfCores;
    }

    public int getConsoleUpdateInterval() {
        return consoleUpdateInterval;
    }

    public int getLogUpdateInterval() {
        return logUpdateInterval;
    }

    public Path getModel() {
        return model;
    }

    private Config() {
        userDir = Paths.get(System.getProperty("user.dir"));
        glmtkDir =
                Paths.get(System.getProperty("glmtk.dir", userDir.toString()));
        logDir = glmtkDir.resolve(Constants.LOG_DIR_NAME);

        Path file = glmtkDir.resolve(Constants.CONFIG_LOCATION);

        try {
            loadConfig(file);
        } catch (Exception e) {
            // Because of enum nature it is necessary to not throw any checked
            // exceptions during construction.
            throw new RuntimeException(e);
        }
    }

    private void loadConfig(Path file) throws IOException, Exception {
        Map<String, Field> fields = new HashMap<String, Field>();
        for (Field field : Config.class.getDeclaredFields()) {
            if (isNotConfigurableField(field)) {
                continue;
            }
            fields.put(field.getName(), field);
        }

        try (BufferedReader reader =
                Files.newBufferedReader(file, Constants.CHARSET)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                ++lineNo;
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }

                List<String> keyValue = StringUtils.splitAtChar(line, '=');
                if (keyValue.size() != 2) {
                    throw error(file, line, lineNo,
                            "Entrys have to be in the form of '<key> = <value>'.");
                }

                String key = keyValue.get(0).trim();
                String value = keyValue.get(1).trim();

                if (!fields.containsKey(key)) {
                    throw error(file, line, lineNo,
                            String.format("Unknown key '%s'.", key));
                }
                Field field = fields.get(key);
                if (field == null) {
                    throw error(file, line, lineNo,
                            String.format("Duplicated key '%s'.", key));
                }

                try {
                    if (field.getType().equals(int.class)
                            || field.getType().equals(Integer.class)) {
                        try {
                            field.set(this, Integer.valueOf(value));
                        } catch (NumberFormatException e) {
                            throw error(file, line, lineNo, String.format(
                                    "Expected number, found '%s'.", value));
                        }
                    } else if (field.getType().equals(Path.class)) {
                        Path path = Paths.get(value);
                        if (path == null) {
                            throw error(file, line, lineNo, String.format(
                                    "Expected path, found '%s'.", value));
                        }
                        field.set(this, path);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // Shouldn't be possible
                    throw new IllegalStateException(e);
                }

                fields.put(key, null);
            }
        }

        for (Entry<String, Field> entry : fields.entrySet()) {
            if (entry.getValue() != null) {
                throw error(file,
                        String.format("Missing key '%s'.", entry.getKey()));
            }
        }
    }

    private boolean isNotConfigurableField(Field field) {
        int mod = field.getModifiers();
        return Modifier.isStatic(mod) || Modifier.isFinal(mod)
                || field.isEnumConstant();
    }

    private Exception error(Path file, String line, int lineNo, String msg)
            throws Exception {
        return new Exception(
                String.format(
                        "Invalid config file '%s' entry at line '%d'. %s Line was: '%s'.",
                        file, lineNo, msg, line));
    }

    private Exception error(Path file, String msg) {
        return new Exception(String.format("Invalid config file '%s'. %s",
                file, msg));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Field field : Config.class.getDeclaredFields()) {
            if (isNotConfigurableField(field)) {
                continue;
            }

            Object value = null;
            try {
                value = field.get(this);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // Shouldn't be possible
                throw new IllegalStateException(e);
            }

            result.append(field.getName());
            result.append('=');
            result.append(value);
            result.append("; ");
        }
        return result.toString();
    }

}
