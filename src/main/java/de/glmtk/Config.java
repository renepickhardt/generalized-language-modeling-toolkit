package de.glmtk;

import java.io.BufferedReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.glmtk.utils.StringUtils;

public class Config {

    private static Config instance = null;

    public static Config getInstance() throws Exception {
        if (instance == null) {
            instance = new Config(Paths.get(Constants.CONFIG_LOCATION));
        }
        return instance;
    }

    /**
     * The directory the user started the program from.
     */
    private Path userDir;

    /**
     * The directory where the GLMTK bundle resides (e.g. directory where config
     * file is).
     */
    private Path glmtkDir;

    /**
     * The directory where log files are saved.
     */
    private Path logDir;

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

    private Config(
            Path file) throws Exception {
        userDir = Paths.get(System.getProperty("user.dir"));
        glmtkDir =
                Paths.get(System.getProperty("glmtk.dir", userDir.toString()));
        logDir = glmtkDir.resolve(Constants.LOG_DIR_NAME);

        file = glmtkDir.resolve(file);

        Map<String, Field> fields = new HashMap<String, Field>();
        for (Field field : Config.class.getDeclaredFields()) {
            if (field.getName().equals("userDir")
                    || field.getName().equals("glmtkDir")
                    || field.getName().equals("logDir")) {
                continue;
            }
            fields.put(field.getName(), field);
        }

        try (BufferedReader reader =
                Files.newBufferedReader(file, Charset.defaultCharset())) {
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
                    throw error(file, line, lineNo, "Unknown key '" + key
                            + "'.");
                }
                Field field = fields.get(key);
                if (field == null) {
                    throw error(file, line, lineNo, "Duplicated key '" + key
                            + "'.");
                }

                try {
                    if (field.getType().equals(int.class)
                            || field.getType().equals(Integer.class)) {
                        try {
                            field.set(this, Integer.valueOf(value));
                        } catch (NumberFormatException e) {
                            throw error(file, line, lineNo,
                                    "Expected number, found '" + value + "'.");
                        }
                    } else if (field.getType().equals(Path.class)) {
                        Path path = Paths.get(value);
                        if (path == null) {
                            throw error(file, line, lineNo,
                                    "Expected path, found '" + value + "'.");
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
    }

    private Exception error(Path file, String line, int lineNo, String msg)
            throws Exception {
        throw new Exception("Invalid config file '" + file
                + "' entry at line '" + lineNo + "'. " + msg + " Line was: '"
                + line + "'.");
    }
}
