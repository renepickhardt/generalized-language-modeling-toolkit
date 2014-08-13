package de.glmtk.pattern;

import java.io.IOException;
import java.util.ArrayList;
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
        // todo: doesn't error on PatternElem#fromString == null
        this.pattern = new ArrayList<PatternElem>(pattern.length());
        for (Character elem : pattern.toCharArray()) {
            this.pattern.add(PatternElem.fromString(elem.toString()));
        }
    }

    public String apply(String[] words, String[] pos) {
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

            result.append(elem.apply(words[i], pos[i]));

            ++i;
        }

        return result.toString();
    }

    public int length() {
        return pattern.size();
    }

    public PatternElem get(int index) {
        return pattern.get(index);
    }

    public PatternElem getFirstNonSkp() {
        for (PatternElem elem : pattern) {
            if (!elem.equals(PatternElem.SKP)) {
                return elem;
            }
        }
        return PatternElem.SKP;
    }

    public void set(int index, PatternElem elem) {
        pattern.set(index, elem);
    }

    public boolean containsPos() {
        for (PatternElem elem : pattern) {
            if (elem.equals(PatternElem.POS)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsSkp() {
        for (PatternElem elem : pattern) {
            if (elem.equals(PatternElem.SKP) || elem.equals(PatternElem.WSKP)
                    || elem.equals(PatternElem.PSKP)
                    || elem.equals(PatternElem.WPOS)) {
                return true;
            }
        }
        return false;
    }

    public boolean onlySkp() {
        for (PatternElem elem : pattern) {
            if (!(elem.equals(PatternElem.SKP) || elem.equals(PatternElem.WSKP)
                    || elem.equals(PatternElem.PSKP) || elem
                        .equals(PatternElem.WPOS))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether pattern only contains CNT and SKP.
     */
    public boolean isAbsolute() {
        for (PatternElem elem : pattern) {
            if (elem.equals(PatternElem.WSKP) || elem.equals(PatternElem.PSKP)
                    || elem.equals(PatternElem.WPOS)) {
                return false;
            }
        }
        return true;
    }

    public Pattern replace(PatternElem target, PatternElem replacement) {
        Pattern newPattern = clone();
        for (int i = newPattern.length() - 1; i != -1; --i) {
            if (newPattern.get(i).equals(target)) {
                newPattern.set(i, replacement);
            }
        }
        return newPattern;
    }

    public Pattern replaceLast(PatternElem target, PatternElem replacement) {
        Pattern newPattern = clone();
        for (int i = newPattern.length() - 1; i != -1; --i) {
            if (newPattern.get(i).equals(target)) {
                newPattern.set(i, replacement);
                break;
            }
        }
        return newPattern;
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
    public int hashCode() {
        int hash = 93485;
        int mult = 239;

        hash += mult * pattern.hashCode();

        return hash;
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

    public static int getModelLength(Set<Pattern> patterns) {
        int modelLength = 0;
        for (Pattern pattern : patterns) {
            if (pattern.length() > modelLength) {
                modelLength = pattern.length();
            }
        }
        return modelLength;
    }

    public static Set<Pattern> getCombinations(
            int modelLength,
            List<PatternElem> elems) {
        Set<Pattern> patterns = new HashSet<Pattern>();

        for (int i = 1; i != modelLength + 1; ++i) {
            for (int j = 0; j != pow(elems.size(), i); ++j) {
                List<PatternElem> pattern = new ArrayList<PatternElem>(i);
                int n = j;
                for (int k = 0; k != i; ++k) {
                    pattern.add(elems.get(n % elems.size()));
                    n /= elems.size();
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

    public static Set<Pattern> replaceTargetWithElems(
            Set<Pattern> inputPatterns,
            PatternElem target,
            List<PatternElem> elems) throws IOException {
        Set<Pattern> patterns = new HashSet<Pattern>();

        for (Pattern pattern : inputPatterns) {
            if (pattern.containsSkp()) {
                for (PatternElem elem : elems) {
                    patterns.add(pattern.replace(target, elem));
                }
            }
        }

        return patterns;
    }

    public static Pattern getContinuationSourcePattern(Pattern pattern) {
        Pattern sourcePattern = pattern.clone();
        for (int i = sourcePattern.length() - 1; i != -1; --i) {
            PatternElem elem = sourcePattern.get(i);
            if (elem.equals(PatternElem.WSKP)) {
                sourcePattern.set(i, PatternElem.CNT);
                break;
            } else if (elem.equals(PatternElem.PSKP)) {
                sourcePattern.set(i, PatternElem.POS);
                break;
            }
        }
        return sourcePattern;
    }

    // Legacy //////////////////////////////////////////////////////////////////

    @Deprecated
    public String apply(Object[] words) {
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

            result.append(elem.apply((String) words[i]));

            ++i;
        }

        return result.toString();
    }

    @Deprecated
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

}
