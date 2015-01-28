/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.common;

import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.CSKIP_ELEMS;
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
public class Pattern implements Iterable<PatternElem>, Comparable<Pattern> {
    public static final Pattern WSKP_PATTERN = Patterns.get(WSKP);

    private List<PatternElem> elems;
    private String asString;

    /* package */Pattern(List<PatternElem> elems,
                         String asString) {
        this.elems = elems;
        this.asString = asString;
    }

    @Override
    public int hashCode() {
        return asString.hashCode();
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
    public int compareTo(Pattern other) {
        int cmp = Integer.compare(numElems(CSKIP_ELEMS),
                other.numElems(CSKIP_ELEMS));
        if (cmp != 0)
            return cmp;
        cmp = Integer.compare(asString.length(), other.asString.length());
        if (cmp != 0)
            return cmp;
        return asString.compareTo(other.asString);
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

    public List<PatternElem> getElems() {
        return new ArrayList<>(elems);
    }

    public PatternElem[] getElemsArray() {
        return elems.toArray(new PatternElem[elems.size()]);
    }

    public PatternElem get(int index) {
        if (index < 0 || index >= size())
            throw new IllegalArgumentException(String.format(
                    "Illegal index: %d. Size: %d.", index, size()));
        return elems.get(index);
    }

    // TODO: untested
    public Pattern set(int index,
                       PatternElem elem) {
        if (index < 0 || index >= size())
            throw new IllegalArgumentException(String.format(
                    "Illegal index: %d. Size: %d.", index, size()));
        List<PatternElem> elems = new ArrayList<>(this.elems);
        elems.set(index, elem);
        return Patterns.get(elems);
    }

    public Pattern remove(int index) {
        if (index < 0 || index >= size())
            throw new IllegalArgumentException(String.format(
                    "Illegal index: %d. Size: %d.", index, size()));
        List<PatternElem> elems = new ArrayList<>(this.elems);
        elems.remove(index);
        return Patterns.get(elems);
    }

    public PatternElem getFirstNonSkp() {
        for (PatternElem elem : elems)
            if (!elem.equals(SKP))
                return elem;
        return SKP;
    }

    public boolean contains(PatternElem elem) {
        return elems.contains(elem);
    }

    public boolean containsAny(Collection<PatternElem> elems) {
        if (elems.isEmpty())
            throw new IllegalArgumentException("Argument was empty collection.");
        for (PatternElem elem : elems)
            if (this.elems.contains(elem))
                return true;
        return false;
    }

    public boolean containsOnly(PatternElem elem) {
        for (PatternElem e : elems)
            if (!e.equals(elem))
                return false;
        return true;
    }

    public boolean containsOnly(Collection<PatternElem> elems) {
        if (elems.isEmpty())
            throw new IllegalArgumentException("Argument was empty collection.");
        outerLoop:
        for (PatternElem e : this.elems) {
            for (PatternElem elem : elems)
                if (e.equals(elem))
                    continue outerLoop;
            return false;
        }
        return true;
    }

    // TODO: untested
    public boolean containsAll(Collection<PatternElem> elems) {
        if (elems.isEmpty())
            throw new IllegalArgumentException("Argument was empty collection.");
        outerLoop:
        for (PatternElem e : elems) {
            for (PatternElem elem : this.elems)
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
        return containsAny(Arrays.asList(POS, PSKP));
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

    /**
     * TODO: rename to "add"?
     */
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

        if (from == 0 && to == size())
            return this;

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

    public Pattern convertSkpToWskp() {
        return replace(SKP, WSKP);
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
