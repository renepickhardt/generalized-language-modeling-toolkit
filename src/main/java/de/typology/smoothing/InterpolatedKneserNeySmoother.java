package de.typology.smoothing;

import java.util.List;

public class InterpolatedKneserNeySmoother extends Smoother {

    @Override
    protected double propability_given(String word, List<String> givenSequence) {
        return 1;
    }

}
