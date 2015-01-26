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

import static de.glmtk.common.BackoffMode.DEL;
import static de.glmtk.common.BackoffMode.DEL_FRONT;
import static de.glmtk.common.BackoffMode.SKP;
import static de.glmtk.common.BackoffMode.SKP_AND_DEL;
import static de.glmtk.common.PatternElem.SKP_WORD;
import static de.glmtk.common.PatternElem.WSKP_WORD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.glmtk.cache.Cache;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.util.StringUtils;

/**
 * Immutable.
 */
public class NGram {
    public static final NGram SKP_NGRAM = new NGram(SKP_WORD);
    public static final NGram WSKP_NGRAM = new NGram(WSKP_WORD);

    private final List<String> words;
    private String asString;
    private Pattern pattern;

    public NGram() {
        words = new ArrayList<>();
        asString = "";
        pattern = Patterns.get();
    }

    public NGram(String word) {
        words = Arrays.asList(word);
        asString = word;
        pattern = Patterns.get(PatternElem.fromWord(word));
    }

    public NGram(List<String> words) {
        this.words = words;
        asString = StringUtils.join(words, " ");

        List<PatternElem> patternElems = new ArrayList<>(words.size());
        for (String word : words)
            patternElems.add(PatternElem.fromWord(word));
        pattern = Patterns.get(patternElems);
    }

    private NGram(List<String> words,
                  Pattern pattern) {
        this.words = words;
        asString = StringUtils.join(words, " ");
        this.pattern = pattern;
    }

    private NGram(List<String> words,
                  String asString,
                  Pattern pattern) {
        this.words = words;
        this.asString = asString;
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        else if (other == null || getClass() != other.getClass())
            return false;

        NGram o = (NGram) other;
        return words.equals(o.words);
    }

    @Override
    public int hashCode() {
        return asString.hashCode();
    }

    @Deprecated
    public List<String> toWordList() {
        return words;
    }

    @Override
    public String toString() {
        return asString;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Returns the order of the ngram. Namely the {@code n}.
     *
     * <p>
     * Does not return number of characters in the ngram.
     *
     * <p>
     * TODO: Should this method thus be renamed?
     */
    public int size() {
        return words.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public NGram get(int index) {
        return new NGram(words.get(index));
    }

    public NGram set(int index,
                     String word) {
        if (index < 0 || index >= size())
            throw new IllegalArgumentException(String.format(
                    "Illegal index: %d. Size: %d.", index, size()));
        List<String> words = new ArrayList<>(this.words);
        words.set(index, word);
        Pattern pattern = this.pattern.set(index, PatternElem.fromWord(word));
        return new NGram(words, pattern);
    }

    public boolean isEmptyOrOnlySkips() {
        if (isEmpty())
            return true;
        for (String word : words)
            if (!word.equals(SKP_WORD))
                return false;
        return true;
    }

    /**
     * Returns {@code true} if is either empty or has count greater zero.
     */
    // TODO: move method into class Cache?
    public boolean seen(Cache cache) {
        return isEmpty() || cache.getAbsolute(this) != 0;
    }

    /**
     * Same as {@link #seen(Cache)} but instead of querying absolute counts,
     * queries alpha counts.
     *
     * <p>
     * This is used in cases were we can't afford to load absolute counts into
     * cache, but have alpha counts present.
     */
    public boolean seenUsingAlphas(Cache cache,
                                   String model) {
        return isEmpty() || cache.getAlpha(model, this) != null;
    }

    public NGram concat(String word) {
        List<String> resultWords = new ArrayList<>(words);
        resultWords.add(word);
        return new NGram(resultWords, asString + " " + word,
                pattern.concat(PatternElem.fromWord(word)));
    }

    public NGram concat(NGram other) {
        List<String> resultWords = new ArrayList<>(words);
        resultWords.addAll(other.words);
        return new NGram(resultWords);
    }

    public NGram range(int from,
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

        List<String> resultWords = new ArrayList<>(to - from);
        List<PatternElem> resultPatternElems = new ArrayList<>(to - from);

        for (int i = from; i != to; ++i) {
            resultWords.add(words.get(i));
            resultPatternElems.add(pattern.get(i));
        }

        return new NGram(resultWords, Patterns.get(resultPatternElems));
    }

    public NGram replace(String target,
                         String replacement) {
        List<String> resultWords = new ArrayList<>(size());

        for (String word : words)
            if (word.equals(target))
                resultWords.add(replacement);
            else
                resultWords.add(word);

        return new NGram(resultWords);
    }

    // TODO: rename to convertToContinuation?
    public NGram convertSkpToWskp() {
        return replace(SKP_WORD, WSKP_WORD);
    }

    public NGram backoff(BackoffMode backoffMode) {
        if (isEmpty())
            throw new IllegalStateException("Can't backoff empty ngrams.");

        // TODO: Rene, is this really correct?
        switch (backoffMode) {
            case SKP:
                List<String> resultWords = new ArrayList<>(words.size());

                boolean replaced = false;
                for (String word : words)
                    if (!replaced && !word.equals(SKP_WORD)) {
                        resultWords.add(SKP_WORD);
                        replaced = true;
                    } else
                        resultWords.add(word);
                if (!replaced)
                    throw new IllegalStateException(
                            "Can't backoff ngrams containing only skips.");
                return new NGram(resultWords);

            case DEL:
                // TODO: optimize with range?
                return new NGram(words.subList(1, words.size()));

            case DEL_FRONT:
            case SKP_AND_DEL:
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    /**
     * Backoffs {@code sequence} at least once, and then until absolute count of
     * it is greater zero. If not possible returns zero. Returned sequence may
     * be empty.
     */
    public NGram backoffUntilSeen(BackoffMode backoffMode,
                                  Cache cache) {
        NGram result = backoff(backoffMode);
        while (!result.seen(cache))
            result = result.backoff(backoffMode);
        return result;
    }

    /**
     * Same as {@link #backoff(BackoffMode)} but instead of querying absolute
     * counts, queries alpha counts.
     *
     * <p>
     * This is used in cases were we can't afford to load absolute counts into
     * cache, but have alpha counts present.
     */
    public NGram backoffUntilSeenUsingAlphas(BackoffMode backoffMode,
                                             Cache cache,
                                             String model) {
        NGram result = backoff(backoffMode);
        while (!result.seenUsingAlphas(cache, model))
            result = result.backoff(backoffMode);
        return result;
    }

    public Set<NGram> getDifferentiatedNGrams(BackoffMode backoffMode) {
        Set<NGram> result = new LinkedHashSet<>();
        for (int i = 0; i != size(); ++i) {
            if (backoffMode == SKP_AND_DEL || backoffMode == DEL
                    || (backoffMode == DEL_FRONT && i == 0)) {
                List<String> newWords = new LinkedList<>(words);
                newWords.remove(i);
                result.add(new NGram(newWords));
            }
            if ((backoffMode == SKP || backoffMode == SKP_AND_DEL || (backoffMode == DEL_FRONT && i != 0))
                    && !words.get(i).equals(SKP_WORD)) {
                List<String> newWords = new LinkedList<>(words);
                newWords.set(i, SKP_WORD);
                result.add(new NGram(newWords));
            }
        }
        return result;
    }
}
