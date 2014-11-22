package de.glmtk.utils;

import static de.glmtk.utils.PatternElem.CNT;
import static de.glmtk.utils.PatternElem.DEL;
import static de.glmtk.utils.PatternElem.POS;
import static de.glmtk.utils.PatternElem.PSKP;
import static de.glmtk.utils.PatternElem.SKP;
import static de.glmtk.utils.PatternElem.WSKP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Immutable.
 */
public class Pattern implements Iterable<PatternElem> {

    // TODO: Test this class.
    // TODO: maybe make all constructors private and use system like
    // Pattern.get() to only have each pattern exists at max once in memory.

    private List<PatternElem> elems;

    private String asString;

    public Pattern() {
        elems = new ArrayList<PatternElem>();
        asString = "";
    }

    public Pattern(
            PatternElem elem) {
        elems = Arrays.asList(elem);
        asString = elem.toString();
    }

    public Pattern(
            List<PatternElem> elems) {
        this.elems = elems;

        StringBuilder asStringBuilder = new StringBuilder();
        for (PatternElem elem : elems) {
            asStringBuilder.append(elem.toString());
        }
        asString = asStringBuilder.toString();
    }

    public Pattern(
            String asString) {
        elems = new ArrayList<PatternElem>(asString.length());
        for (char elemAsChar : asString.toCharArray()) {
            PatternElem elem = PatternElem.fromChar(elemAsChar);
            if (elem == null) {
                throw new IllegalStateException("Unkown PatternElem: '"
                        + elemAsChar + "'.");
            }
            elems.add(elem);
        }

        this.asString = asString;
    }

    private Pattern(
            List<PatternElem> elems,
            String asString) {
        this.elems = elems;
        this.asString = asString;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null || !other.getClass().equals(Pattern.class)) {
            return false;
        }

        Pattern o = (Pattern) other;
        return asString.equals(o.asString);
    }

    @Override
    public int hashCode() {
        return asString.hashCode();
    }

    @Override
    public String toString() {
        return asString;
    }

    @Override
    public Iterator<PatternElem> iterator() {
        return elems.iterator();
    }

    public int size() {
        return elems.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public PatternElem get(int index) {
        return elems.get(index);
    }

    public PatternElem getFirstNonSkp() {
        for (PatternElem elem : elems) {
            if (!elem.equals(SKP)) {
                return elem;
            }
        }
        return SKP;
    }

    public boolean contains(PatternElem elem) {
        return contains(Arrays.asList(elem));
    }

    public boolean contains(Collection<PatternElem> elems) {
        for (PatternElem elem : this.elems) {
            for (PatternElem e : elems) {
                if (elem.equals(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsOnly(PatternElem elem) {
        return containsOnly(Arrays.asList(elem));
    }

    public boolean containsOnly(Collection<PatternElem> elems) {
        outerLoop:
            for (PatternElem elem : this.elems) {
            for (PatternElem e : elems) {
                if (elem.equals(e)) {
                    continue outerLoop;
                }
            }
            return false;
        }
    return true;
    }

    public boolean isAbsolute() {
        return containsOnly(Arrays.asList(CNT, SKP, POS));
    }

    public int numElems(Collection<PatternElem> elems) {
        int result = 0;
        outerLoop:
        for (PatternElem elem : this.elems) {
            for (PatternElem e : elems) {
                if (elem.equals(e)) {
                    ++result;
                    continue outerLoop;
                }
            }
        }
        return result;
    }

    public Pattern concat(PatternElem elem) {
        List<PatternElem> resultElems = new ArrayList<PatternElem>(elems);
        resultElems.add(elem);
        return new Pattern(resultElems, asString + elem.toChar());
    }

    public Pattern concat(Pattern other) {
        List<PatternElem> resultElems = new ArrayList<PatternElem>(elems);
        resultElems.addAll(other.elems);
        return new Pattern(resultElems, asString + other.asString);
    }

    public Pattern replace(PatternElem target, PatternElem replacement) {
        List<PatternElem> resultElems = new ArrayList<PatternElem>(size());
        StringBuilder resultAsString = new StringBuilder();

        for (PatternElem elem : elems) {
            if (elem.equals(target)) {
                resultElems.add(replacement);
                resultAsString.append(replacement.toChar());
            } else {
                resultElems.add(elem);
                resultAsString.append(elem.toChar());
            }
        }

        return new Pattern(resultElems, resultAsString.toString());
    }

    public Pattern replaceLast(PatternElem target, PatternElem replacement) {
        List<PatternElem> resultElems = new ArrayList<PatternElem>(elems);

        for (int i = size() - 1; i != -1; --i) {
            if (elems.get(i).equals(target)) {
                resultElems.set(i, replacement);
            }
        }

        return new Pattern(resultElems);
    }

    public Pattern getContinuationSource() {
        List<PatternElem> resultElems = new ArrayList<PatternElem>(size());

        for (int i = size() - 1; i != -1; --i) {
            PatternElem elem = get(i);
            if (elem.equals(WSKP)) {
                resultElems.set(i, CNT);
                return new Pattern(resultElems);
            } else if (elem.equals(PSKP)) {
                resultElems.set(i, POS);
                return new Pattern(resultElems);
            }
        }

        throw new IllegalArgumentException("Pattern '" + this
                + "' is not a continuation pattern.");
    }

    public String apply(String[] words) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        Iterator<PatternElem> it = elems.iterator();
        for (int i = 0; i != size(); ++i) {
            PatternElem elem = it.next();

            if (elem != DEL) {
                if (!first) {
                    result.append(' ');
                }
                first = false;
            }

            result.append(elem.apply(words[i]));
        }

        return result.toString();
    }

    public String apply(String[] words, String[] pos, int p) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        Iterator<PatternElem> it = elems.iterator();
        for (int i = 0; i != size(); ++i) {
            PatternElem elem = it.next();

            if (elem != DEL) {
                if (!first) {
                    result.append(' ');
                }
                first = false;
            }

            result.append(elem.apply(words[p + i], pos[p + i]));
        }

        return result.toString();
    }

    public static Set<Pattern> getCombinations(
            int modelSize,
            List<PatternElem> elems) {
        Set<Pattern> patterns = new HashSet<Pattern>();

        for (int i = 1; i != modelSize + 1; ++i) {
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

}
