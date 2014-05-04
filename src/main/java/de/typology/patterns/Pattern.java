package de.typology.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Pattern implements Iterable<PatternType>, Cloneable {

    private List<PatternType> pattern;

    public Pattern(
            List<PatternType> pattern) {
        this.pattern = pattern;
    }

    public Pattern(
            String label) {
        pattern = new ArrayList<PatternType>(label.length());
        for (Character l : label.toCharArray()) {
            pattern.add(PatternType.fromString(l.toString()));
        }
    }

    public int length() {
        return pattern.size();
    }

    public PatternType get(int index) {
        return pattern.get(index);
    }

    public void set(int index, PatternType type) {
        pattern.set(index, type);
    }

    public int numCnt() {
        int result = 0;
        for (PatternType type : pattern) {
            if (type == PatternType.CNT) {
                ++result;
            }
        }
        return result;
    }

    public boolean containsNoSkp() {
        for (PatternType type : pattern) {
            if (type == PatternType.SKP) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Pattern clone() {
        return new Pattern(new ArrayList<PatternType>(pattern));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null || other.getClass() != Pattern.class) {
            return false;
        }

        Pattern o = (Pattern) other;
        return pattern.equals(o.pattern);
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

    public static Pattern newWithoutSkp(Pattern old) {
        List<PatternType> pattern = new ArrayList<PatternType>();
        for (PatternType type : old) {
            if (type != PatternType.SKP) {
                pattern.add(type);
            }
        }
        return new Pattern(pattern);
    }

    public static Pattern newWithCnt(int length) {
        List<PatternType> pattern = new ArrayList<PatternType>(length + 1);
        for (int i = 0; i != length + 1; ++i) {
            pattern.add(PatternType.CNT);
        }
        return new Pattern(pattern);
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
            patterns.add(newWithCnt(i));
        }
        return patterns;
    }

}
