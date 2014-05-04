package de.typology.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Pattern implements Iterable<PatternType> {

    private List<PatternType> pattern;

    public Pattern(
            List<PatternType> pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        String result = "";
        for (PatternType patternType : pattern) {
            result += patternType;
        }
        return result;
    }

    @Override
    public Iterator<PatternType> iterator() {
        return pattern.iterator();
    }

    public static List<Pattern> getGlmForSmoothingPatterns(int modelLength) {
        int pow = 1 << modelLength; // 2^modelLength
        List<Pattern> patterns = new ArrayList<Pattern>(pow);
        for (int i = 1; i != pow; ++i) {
            int length = Integer.SIZE - Integer.numberOfLeadingZeros(i);
            List<PatternType> pattern = new ArrayList<PatternType>(length);
            int n = i;
            do {
                pattern.add((n & 1) != 0 ? PatternType.CNT : PatternType.SKP);
            } while ((n >>= 1) != 0);
            Collections.reverse(pattern);
            patterns.add(new Pattern(pattern));
        }
        return patterns;
    }

    public static List<Pattern> getReverseGlmForSmoothingPatterns(
            int modelLength) {
        List<Pattern> patterns = getGlmForSmoothingPatterns(modelLength);
        Collections.reverse(patterns);
        return patterns;
    }

    public static List<Pattern> getReverseLmPatterns(int modelLength) {
        List<Pattern> patterns = new ArrayList<Pattern>(modelLength);
        for (int i = modelLength - 1; i != -1; --i) {
            List<PatternType> pattern = new ArrayList<PatternType>(i + 1);
            for (int j = 0; j != i + 1; ++j) {
                pattern.add(PatternType.CNT);
            }
            patterns.add(new Pattern(pattern));
        }
        return patterns;
    }

}
