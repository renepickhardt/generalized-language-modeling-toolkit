package de.typology.smoothing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class InterpolatedKneserNeySmoother extends Smoother {

    public InterpolatedKneserNeySmoother(
            Path absoluteDir,
            Path continuationDir,
            String delimiter) throws IOException {
        super(absoluteDir, continuationDir, delimiter);
    }

    @Override
    protected double propability_given(String word, List<String> givenSequence) {
        return 1;
    }

}
