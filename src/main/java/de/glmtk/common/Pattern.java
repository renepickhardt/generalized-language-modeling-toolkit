package de.glmtk.common;

import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.DEL;
import static de.glmtk.common.PatternElem.POS;
import static de.glmtk.common.PatternElem.PSKP;
import static de.glmtk.common.PatternElem.SKP;
import static de.glmtk.common.PatternElem.WSKP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Immutable.
 *
 * Tests for this class can be found in {@link PatternTest}.
 */
public class Pattern implements Iterable<PatternElem> {
    private List<PatternElem> elems;
    private String asString;

    /* package */Pattern(List<PatternElem> elems,
                         String asString) {
        this.elems = elems;
        this.asString = asString;
    }

    /**
     * Because all Patterns are cached, and can only be accessed through
     * {@link Patterns#get}, there can never be two instances of equal Patterns,
     * thus it suffices to check for object identity.
     *
     * We could just omit this definition since it's default, but it's left for
     * clarification.
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
        if (index < 0 || index > size())
            throw new IllegalArgumentException(String.format(
                    "Illegal index: %d. Size: %d.", index, size()));
        return elems.get(index);
    }

    public PatternElem getFirstNonSkp() {
        for (PatternElem elem : elems)
            if (!elem.equals(SKP))
                return elem;
        return SKP;
    }

    public boolean contains(PatternElem elem) {
        return contains(Arrays.asList(elem));
    }

    public boolean contains(Collection<PatternElem> elems) {
        if (elems.isEmpty())
            throw new IllegalArgumentException("Argument was empty collection.");
        for (PatternElem elem : this.elems)
            for (PatternElem e : elems)
                if (elem.equals(e))
                    return true;
        return false;
    }

    public boolean containsOnly(PatternElem elem) {
        return containsOnly(Arrays.asList(elem));
    }

    public boolean containsOnly(Collection<PatternElem> elems) {
        if (elems.isEmpty())
            throw new IllegalArgumentException("Argument was empty collection.");
        outerLoop:
            for (PatternElem elem : this.elems) {
            for (PatternElem e : elems)
                if (elem.equals(e))
                    continue outerLoop;
            return false;
        }
        return true;
    }

    public boolean isAbsolute() {
        return containsOnly(Arrays.asList(CNT, SKP, POS));
    }

    public boolean isPos() {
        return contains(Arrays.asList(POS, PSKP));
    }

    public int numElems(Collection<PatternElem> elems) {
        if (elems.isEmpty())
            throw new IllegalArgumentException("Argument was empty collection.");
        int result = 0;
        outerLoop:
        for (PatternElem elem : this.elems)
            for (PatternElem e : elems)
                if (elem.equals(e)) {
                    ++result;
                    continue outerLoop;
                }
        return result;
    }

    public Pattern concat(PatternElem elem) {
        return Patterns.get(asString + elem.toChar());
    }

    public Pattern concat(Pattern other) {
        return Patterns.get(asString + other.asString);
    }

    public Pattern range(int from,
                         int to) {
        if (from < 0 || from > size())
            throw new IllegalArgumentException(String.format(
                    "Illegal from index: %d", from));
        else if (to < 0 || to > size())
            throw new IllegalArgumentException(String.format(
                    "Illegal to index: %d", to));
        else if (from > to)
            throw new IllegalArgumentException(String.format(
                    "From index larger than to index: %d > %d", from, to));

        List<PatternElem> resultElems = new ArrayList<>(to - from);

        for (int i = from; i != to; ++i)
            resultElems.add(elems.get(i));

        return Patterns.get(resultElems);
    }

    public Pattern replace(PatternElem target,
                           PatternElem replacement) {
        StringBuilder resultAsString = new StringBuilder();

        for (PatternElem elem : elems)
            if (elem.equals(target))
                resultAsString.append(replacement.toChar());
            else
                resultAsString.append(elem.toChar());

        return Patterns.get(resultAsString.toString());
    }

    public Pattern replaceLast(PatternElem target,
                               PatternElem replacement) {
        List<PatternElem> resultElems = new ArrayList<>(elems);

        for (int i = size() - 1; i != -1; --i)
            if (elems.get(i).equals(target)) {
                resultElems.set(i, replacement);
                break;
            }

        return Patterns.get(resultElems);
    }

    public Pattern getContinuationSource() {
        List<PatternElem> resultElems = new ArrayList<>(elems);

        for (int i = size() - 1; i != -1; --i) {
            PatternElem elem = get(i);
            if (elem.equals(WSKP)) {
                resultElems.set(i, CNT);
                return Patterns.get(resultElems);
            } else if (elem.equals(PSKP)) {
                resultElems.set(i, POS);
                return Patterns.get(resultElems);
            }
        }

        throw new IllegalArgumentException(String.format(
                "Pattern '%s' is not a continuation pattern.", this));
    }

    // TODO: untested
    public String apply(String[] words) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (int i = 0; i != size(); ++i) {
            PatternElem elem = elems.get(i);

            if (elem != DEL) {
                if (!first)
                    result.append(' ');
                first = false;
            }

            result.append(elem.apply(words[i]));
        }

        return result.toString();
    }

    // TODO: untested
    public String apply(String[] words,
                        String[] pos,
                        int p) {
        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (int i = 0; i != size(); ++i) {
            PatternElem elem = elems.get(i);

            if (elem != DEL) {
                if (!first)
                    result.append(' ');
                first = false;
            }

            result.append(elem.apply(words[p + i], pos[p + i]));
        }

        return result.toString();
    }
}
