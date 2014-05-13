package de.typology.smoothing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public class InterpolatedKneserNeySmoother extends Smoother {

    public InterpolatedKneserNeySmoother(
            Path absoluteDir,
            Path continuationDir,
            String delimiter) throws IOException {
        super(absoluteDir, continuationDir, delimiter);
    }

    @Override
    protected double propability_given(String word, List<String> givenSequence) {
        List<String> history = new LinkedList<String>(givenSequence);
        history.add(PatternElem.SKIPPED_WORD);
    }

    /**
     * @return The discount value for n-grams with {@code length}.
     */
    private double discount(int length) {
        double n_1 = nGramTimesCount(length, 1);
        double n_2 = nGramTimesCount(length, 2);
        return n_1 / (n_1 + 2 * n_2);
    }

    private double lambda(List<String> sequence) {
        // TODO: ask rene if correct absolute count
        List<PatternElem> patternElems =
                new ArrayList<PatternElem>(sequence.size());
        for (String word : sequence) {
            if (word.equals(PatternElem.SKIPPED_WORD)) {
                patternElems.add(PatternElem.SKP);
            } else {
                patternElems.add(PatternElem.CNT);
            }
        }
        Pattern absolutePattern = new Pattern(patternElems);
        Pattern continuationPattern =
                absolutePattern.replace(PatternElem.SKP, PatternElem.WSKP);

        String sequenceString = StringUtils.join(sequence, " ");

        double d = discount(sequence.size());
        double n =
                continuationCounts.get(continuationPattern).get(sequenceString)
                        .getOneCount();
        double c = absoluteCounts.get(absolutePattern).get(sequenceString);

        return d * n / c;
    }

}
