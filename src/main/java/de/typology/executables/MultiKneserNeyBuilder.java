package de.typology.executables;

import java.io.IOException;

import de.typology.utils.LegacyConfig;

public class MultiKneserNeyBuilder {

    public static void main(String[] args) throws IOException,
            InterruptedException {
        String[] languages = LegacyConfig.get().languages.split(",");
        String inputDataSet = LegacyConfig.get().inputDataSet;
        for (String language : languages) {
            LegacyConfig.get().inputDataSet = inputDataSet + "/" + language;
            KneserNeyBuilder.main(args);
        }

    }
}
