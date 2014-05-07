package de.typology.patterns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Pattern implements Iterable<PatternElem>, Cloneable {

    private List<PatternElem> pattern;

    public Pattern(
            List<PatternElem> pattern) {
        this.pattern = pattern;
    }

    public Pattern(
            String pattern) {
        this.pattern = new ArrayList<PatternElem>(pattern.length());
        for (Character elem : pattern.toCharArray()) {
            this.pattern.add(PatternElem.fromString(elem.toString()));
        }
    }

    public int length() {
        return pattern.size();
    }

    public String apply(String[] words, String[] pos, int p) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        int i = 0;
        for (PatternElem elem : pattern) {
            if (elem != PatternElem.DEL) {
                if (!first) {
                    result.append(' ');
                }
                first = false;
            }

            result.append(elem.apply(words[p + i], pos[p + i]));

            ++i;
        }

        return result.toString();
    }

    public PatternElem get(int index) {
        return pattern.get(index);
    }

    public void set(int index, PatternElem elem) {
        pattern.set(index, elem);
    }

    public int numCnt() {
        int result = 0;
        for (PatternElem elem : pattern) {
            if (elem == PatternElem.CNT) {
                ++result;
            }
        }
        return result;
    }

    public boolean containsNoSkp() {
        for (PatternElem elem : pattern) {
            if (elem == PatternElem.SKP) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Pattern clone() {
        return new Pattern(new ArrayList<PatternElem>(pattern));
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
        for (PatternElem elem : pattern) {
            result += elem;
        }
        return result;
    }

    @Override
    public Iterator<PatternElem> iterator() {
        return pattern.iterator();
    }

    public static Pattern newWithoutSkp(Pattern old) {
        List<PatternElem> pattern = new ArrayList<PatternElem>();
        for (PatternElem elem : old) {
            if (elem != PatternElem.SKP) {
                pattern.add(elem);
            }
        }
        return new Pattern(pattern);
    }

    public static Pattern newWithCnt(int length) {
        List<PatternElem> pattern = new ArrayList<PatternElem>(length + 1);
        for (int i = 0; i != length + 1; ++i) {
            pattern.add(PatternElem.CNT);
        }
        return new Pattern(pattern);
    }

    public static Set<Pattern> getCombinations(
            int modelLength,
            PatternElem[] elems) {
        Set<Pattern> patterns = new HashSet<Pattern>();

        for (int i = 1; i != modelLength + 1; ++i) {
            for (int j = 0; j != pow(elems.length, i); ++j) {
                List<PatternElem> pattern = new ArrayList<PatternElem>(i);
                int n = j;
                for (int k = 0; k != i; ++k) {
                    pattern.add(elems[n % elems.length]);
                    n /= elems.length;
                }
                patterns.add(new Pattern(pattern));
            }
        }

        return patterns;
    }

    private static int pow(int base, int power) {
        int result = 1;
        for (int i = 0; i != power; ++i) {
            result *= base;
        }
        return result;
    }

    public static List<Pattern> getGlmForSmoothingPatterns(int modelLength) {
        int pow = 1 << modelLength; // 2^modelLength
        List<Pattern> patterns = new ArrayList<Pattern>(pow);
        for (int i = 1; i != pow; ++i) {
            int length = Integer.SIZE - Integer.numberOfLeadingZeros(i);
            List<PatternElem> pattern = new ArrayList<PatternElem>(length);
            int n = i;
            do {
                pattern.add((n & 1) != 0 ? PatternElem.CNT : PatternElem.SKP);
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
