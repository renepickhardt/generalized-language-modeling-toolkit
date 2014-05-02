package de.typology.filtering;

import java.nio.file.Path;

public class Filterer {

    private Path input;

    private Path outputDirectory;

    public Filterer(
            Path input,
            Path outputDirectory) {
        this.input = input;
        this.outputDirectory = outputDirectory;
    }

}
