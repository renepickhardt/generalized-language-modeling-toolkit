package de.typology.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ini4j.Ini;

public class Config {

    private static final String CONFIG_LOCATION = "config.ini";

    private static Config instance = null;

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

    private Map<String, Map<String, Object>> sections =
            new LinkedHashMap<String, Map<String, Object>>();

    public static Config get() {
        try {
            if (instance == null) {
                instance = new Config();
            }
            return instance;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Config() throws IOException {
        userDir = Paths.get(System.getProperty("user.dir"));
        glmtkDir =
                Paths.get(System.getProperty("glmtk.dir", userDir.toString()));
        logDir = glmtkDir.resolve("logs");

        Ini ini = new Ini();
        ini.load(Files.newBufferedReader(glmtkDir.resolve(CONFIG_LOCATION),
                Charset.defaultCharset()));

        // general
        read(ini, "general", "mainMemory", Integer.class);
        read(ini, "general", "numberOfCores", Integer.class);

        // glmtk-count
        read(ini, "glmtk-count", "model", Path.class);

        // glmtk
    }

    private void read(
            Ini ini,
            String sectionName,
            String optionName,
            Class<?> optionClazz) {
        Map<String, Object> section = sections.get(sectionName);
        if (section == null) {
            section = new LinkedHashMap<String, Object>();
            sections.put(sectionName, section);
        }

        String optionValue = ini.get(sectionName, optionName);
        if (optionValue == null || optionValue == "") {
            throw new IllegalArgumentException("Option \"" + optionName
                    + "\" has missing or empty value.");
        }

        Object value = null;
        if (optionClazz.equals(Integer.class)) {
            value = Integer.parseInt(optionValue);
        } else if (optionClazz.equals(Path.class)) {
            value = Paths.get(optionValue);
        } else {
            throw new IllegalStateException(
                    "Unimplemented read option class + " + optionClazz + ".");
        }

        section.put(optionName, value);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Map<String, Object>> section : sections
                .entrySet()) {
            String sectionName = section.getKey();
            Map<String, Object> sectionOptions = section.getValue();

            result.append(sectionName);
            result.append("{ ");
            for (Map.Entry<String, Object> option : sectionOptions.entrySet()) {
                String optionName = option.getKey();
                Object optionValue = option.getValue();

                result.append(optionName);
                result.append("=");

                if (optionValue instanceof Integer) {
                    Integer value = (Integer) optionValue;
                    result.append(value);
                } else if (optionValue instanceof Path) {
                    Path value = (Path) optionValue;
                    result.append("\"");
                    result.append(value);
                    result.append("\"");
                } else {
                    throw new IllegalStateException(
                            "Unimplemented toString option class: "
                                    + optionValue.getClass() + ".");
                }

                result.append("; ");
            }
            result.append("}; ");
        }
        return result.toString();
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

    // GENERAL /////////////////////////////////////////////////////////////////

    public int getMainMemory() {
        return (int) sections.get("general").get("mainMemory");
    }

    public int getNumberOfCores() {
        return (int) sections.get("general").get("numberOfCores");
    }

    // GLMTK-COUNT /////////////////////////////////////////////////////////////

    public Path getModel() {
        return (Path) sections.get("glmtk-count").get("model");
    }

    // GLMTK ///////////////////////////////////////////////////////////////////

}
