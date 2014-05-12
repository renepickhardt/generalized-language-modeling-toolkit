package de.typology.smoothing;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.typology.counting.Counter;
import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;
import de.typology.utils.StringUtils;

public abstract class Smoother {

    protected Map<Pattern, Map<String, Integer>> absoluteCounts;

    protected Map<Pattern, Map<String, Counter>> continuationCoutns;

    public double propability(String sequence) {
        List<String> words = StringUtils.splitAtSpace(sequence);

        double result = 1;
        for (int i = 0; i != words.size(); ++i) {
            String word = words.get(i);
            List<String> givenSequence =
                    new LinkedList<String>(words.subList(0, i));
            for (int j = 0; j != words.size() - i - 1; ++j) {
                givenSequence.add(PatternElem.SKIPPED_WORD);
            }

            result *= propability_given(word, givenSequence);
        }
        return result;
    }

    protected abstract double propability_given(
            String word,
            List<String> givenSequence);

}
