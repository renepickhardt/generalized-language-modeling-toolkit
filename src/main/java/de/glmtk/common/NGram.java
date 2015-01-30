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

    private final List<String> tokens;
    private String string;
    private Pattern pattern;

    public NGram() {
        tokens = new ArrayList<>();
        string = "";
        pattern = Patterns.get();
    }

    public NGram(String token) {
        tokens = Arrays.asList(token);
        string = token;
        pattern = Patterns.get(PatternElem.fromWord(token));
    }

    public NGram(List<String> tokens) {
        this.tokens = tokens;
        string = StringUtils.join(tokens, " ");

        List<PatternElem> patternElems = new ArrayList<>(tokens.size());
        for (String token : tokens)
            patternElems.add(PatternElem.fromWord(token));
        pattern = Patterns.get(patternElems);
    }

    private NGram(List<String> tokens,
                  Pattern pattern) {
        this.tokens = tokens;
        string = StringUtils.join(tokens, " ");
        this.pattern = pattern;
    }

    private NGram(List<String> tokens,
                  String string,
                  Pattern pattern) {
        this.tokens = tokens;
        this.string = string;
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        else if (other == null || getClass() != other.getClass())
            return false;

        NGram o = (NGram) other;
        return tokens.equals(o.tokens);
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Deprecated
    public List<String> toWordList() {
        return tokens;
    }

    @Override
    public String toString() {
        return string;
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
        return tokens.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public NGram get(int index) {
        return new NGram(tokens.get(index));
    }

    public NGram set(int index,
                     String token) {
        if (index < 0 || index >= size())
            throw new IllegalArgumentException(String.format(
                    "Illegal index: %d. Size: %d.", index, size()));
        List<String> newTokens = new ArrayList<>(tokens);
        newTokens.set(index, token);
        Pattern newPattern = pattern.set(index, PatternElem.fromWord(token));
        return new NGram(newTokens, newPattern);
    }

    public NGram remove(int index) {
        if (index < 0 || index >= size())
            throw new IllegalArgumentException(String.format(
                    "Illegal index: %d. Size: %d.", index, size()));
        List<String> newTokens = new ArrayList<>(tokens);
        newTokens.remove(index);
        Pattern newPattern = pattern.remove(index);
        return new NGram(newTokens, newPattern);
    }

    public boolean isEmptyOrOnlySkips() {
        if (isEmpty())
            return true;
        for (String token : tokens)
            if (!token.equals(SKP_WORD))
                return false;
        return true;
    }

    /**
     * Returns {@code true} if is either empty or has count greater zero.
     */
    // TODO: move method into class Cache?
    public boolean seen(Cache cache) {
        if (isEmpty())
            return true;

        if (pattern.isAbsolute())
            return cache.getAbsolute(this) != 0;
        return cache.getContinuation(this).getOnePlusCount() != 0;
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

    public NGram concat(String token) {
        if (isEmpty())
            return new NGram(token);
        List<String> resultTokens = new ArrayList<>(tokens);
        resultTokens.add(token);
        String newString = string + " " + token;
        Pattern newPattern = pattern.concat(PatternElem.fromWord(token));
        return new NGram(resultTokens, newString, newPattern);
    }

    public NGram concat(NGram other) {
        if (isEmpty())
            return other;
        if (other.isEmpty())
            return this;
        List<String> newTokens = new ArrayList<>(tokens);
        newTokens.addAll(other.tokens);
        String newString = string + " " + other.string;
        Pattern newPattern = pattern.concat(other.pattern);
        return new NGram(newTokens, newString, newPattern);
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

        List<String> newTokens = new ArrayList<>(to - from);
        List<PatternElem> newPattern = new ArrayList<>(to - from);

        for (int i = from; i != to; ++i) {
            newTokens.add(tokens.get(i));
            newPattern.add(pattern.get(i));
        }

        return new NGram(newTokens, Patterns.get(newPattern));
    }

    public NGram replace(String target,
                         String replacement) {
        PatternElem replacementElem = PatternElem.fromWord(replacement);

        List<String> newTokens = new ArrayList<>(tokens);
        List<PatternElem> newPattern = pattern.getElems();

        for (int i = 0; i != size(); ++i) {
            String token = newTokens.get(i);
            if (token.equals(target)) {
                newTokens.set(i, replacement);
                newPattern.set(i, replacementElem);
            }
        }

        return new NGram(newTokens, Patterns.get(newPattern));
    }

    // TODO: rename to "convertToContinuation"?
    public NGram convertSkpToWskp() {
        return replace(SKP_WORD, WSKP_WORD);
    }

    public NGram backoff(BackoffMode backoffMode) {
        if (isEmpty())
            throw new IllegalStateException("Can't backoff empty ngrams.");

        switch (backoffMode) {
            case SKP:
                for (int i = 0; i != tokens.size(); ++i)
                    if (!tokens.get(i).equals(SKP_WORD))
                        return set(i, SKP_WORD);
                throw new IllegalStateException(
                        "Can't backoff ngrams containing only skips.");

            case DEL:
                return remove(0);

            case DEL_FRONT:
            case SKP_AND_DEL:
                throw new IllegalArgumentException();

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
                    || (backoffMode == DEL_FRONT && i == 0))
                result.add(remove(i));
            if ((backoffMode == SKP || backoffMode == SKP_AND_DEL || (backoffMode == DEL_FRONT && i != 0))
                    && !tokens.get(i).equals(SKP_WORD))
                result.add(set(i, SKP_WORD));
        }
        return result;
    }
}
