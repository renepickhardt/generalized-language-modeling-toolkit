package de.typology.executables;

import java.io.IOException;

import de.typology.utils.Config;

public class MultiKneserNeyBuilder {

    public static void main(String[] args) throws IOException {
        String[] languages = Config.get().languages.split(",");
        String inputDataSet = Config.get().inputDataSet;
        for (String language : languages) {
            Config.get().inputDataSet = inputDataSet + "/" + language;
            KneserNeyBuilder.main(args);
        }

    }
}
