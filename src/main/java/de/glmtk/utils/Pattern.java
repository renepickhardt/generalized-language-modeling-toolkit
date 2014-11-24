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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable.
 *
 * Tests for this class can be found in {@link PatternTest}.
 */
public class Pattern implements Iterable<PatternElem> {

    private List<PatternElem> elems;

    private String asString;

    private Pattern(
            List<PatternElem> elems,
            String asString) {
        this.elems = elems;
        this.asString = asString;
    }

    /**
     * Because all Patterns are cached, and can only be accessed through
     * {@link Pattern#get()}, there can never be two instances of equal
     * Patterns, thus it suffices to check for object identity.
     *
     * We could just omit this definition since it's default, but it's left
     * for clarification.
     */
    @Override
    public boolean equals(Object other) {
        return this == other;
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
        if (index < 0 || index > size()) {
            throw new IllegalArgumentException("Illegal index: " + index
                    + ". Size: " + size() + ".");
        }
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
        if (elems.isEmpty()) {
            throw new IllegalArgumentException("Argument was empty collection.");
        }
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
        if (elems.isEmpty()) {
            throw new IllegalArgumentException("Argument was empty collection.");
        }
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
        if (elems.isEmpty()) {
            throw new IllegalArgumentException("Argument was empty collection.");
        }
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
        return Pattern.get(asString + elem.toChar());
    }

    public Pattern concat(Pattern other) {
        return Pattern.get(asString + other.asString);
    }

    public Pattern range(int from, int to) {
        if (from < 0 || from > size()) {
            throw new IllegalArgumentException("Illegal from index: " + from);
        } else if (to < 0 || to > size()) {
            throw new IllegalArgumentException("Illegal to index: " + to);
        } else if (from > to) {
            throw new IllegalArgumentException(
                    "From index larger or equal than to index: " + from
                            + " >= " + to);
        }

        List<PatternElem> resultElems = new ArrayList<PatternElem>(to - from);

        for (int i = from; i != to; ++i) {
            resultElems.add(elems.get(i));
        }

        return Pattern.get(resultElems);
    }

    public Pattern replace(PatternElem target, PatternElem replacement) {
        StringBuilder resultAsString = new StringBuilder();

        for (PatternElem elem : elems) {
            if (elem.equals(target)) {
                resultAsString.append(replacement.toChar());
            } else {
                resultAsString.append(elem.toChar());
            }
        }

        return Pattern.get(resultAsString.toString());
    }

    public Pattern replaceLast(PatternElem target, PatternElem replacement) {
        List<PatternElem> resultElems = new ArrayList<PatternElem>(elems);

        for (int i = size() - 1; i != -1; --i) {
            if (elems.get(i).equals(target)) {
                resultElems.set(i, replacement);
                break;
            }
        }

        return Pattern.get(resultElems);
    }

    public Pattern getContinuationSource() {
        List<PatternElem> resultElems = new ArrayList<PatternElem>(elems);

        for (int i = size() - 1; i != -1; --i) {
            PatternElem elem = get(i);
            if (elem.equals(WSKP)) {
                resultElems.set(i, CNT);
                return Pattern.get(resultElems);
            } else if (elem.equals(PSKP)) {
                resultElems.set(i, POS);
                return Pattern.get(resultElems);
            }
        }

        throw new IllegalArgumentException("Pattern '" + this
                + "' is not a continuation pattern.");
    }

    // TODO: untested
    public String apply(String[] words) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (int i = 0; i != size(); ++i) {
            PatternElem elem = elems.get(i);

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

    // TODO: untested
    public String apply(String[] words, String[] pos, int p) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (int i = 0; i != size(); ++i) {
            PatternElem elem = elems.get(i);

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

    private static final Map<String, Pattern> AS_STRING_TO_PATTERN =
            new HashMap<String, Pattern>();

    public static Pattern get() {
        Pattern pattern = AS_STRING_TO_PATTERN.get("");
        if (pattern == null) {
            pattern = new Pattern(new ArrayList<PatternElem>(), "");
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(PatternElem elem) {
        Pattern pattern = AS_STRING_TO_PATTERN.get(elem.toString());
        if (pattern == null) {
            pattern = new Pattern(Arrays.asList(elem), elem.toString());
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(List<PatternElem> elems) {
        StringBuilder asStringBuilder = new StringBuilder();
        for (PatternElem elem : elems) {
            asStringBuilder.append(elem.toString());
        }
        String asString = asStringBuilder.toString();

        Pattern pattern = AS_STRING_TO_PATTERN.get(asString);
        if (pattern == null) {
            pattern = new Pattern(elems, asString);
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(String asString) {
        Pattern pattern = AS_STRING_TO_PATTERN.get(asString);
        if (pattern == null) {
            List<PatternElem> elems =
                    new ArrayList<PatternElem>(asString.length());
            for (char elemAsChar : asString.toCharArray()) {
                PatternElem elem = PatternElem.fromChar(elemAsChar);
                if (elem == null) {
                    throw new IllegalStateException("Unkown PatternElem: '"
                            + elemAsChar + "'.");
                }
                elems.add(elem);
            }

            pattern = new Pattern(elems, asString);
            cachePattern(pattern);
        }
        return pattern;
    }

    private static void cachePattern(Pattern pattern) {
        AS_STRING_TO_PATTERN.put(pattern.asString, pattern);
    }

    // TODO: untested
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
                patterns.add(Pattern.get(pattern));
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
