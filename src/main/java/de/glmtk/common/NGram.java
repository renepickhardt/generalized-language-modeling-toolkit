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

import de.glmtk.util.StringUtils;

/**
 * Immutable.
 */
public class NGram {

    // TODO: Test this class;
    // TODO: Replace 'word' with 'token'.

    public static final NGram SKP_NGRAM = new NGram(SKP_WORD);

    public static final NGram WSKP_NGRAM = new NGram(WSKP_WORD);

    private final List<String> words;

    private String asString;

    private Pattern pattern;

    public NGram() {
        words = new ArrayList<String>();
        asString = "";
        pattern = Patterns.get();
    }

    public NGram(
            String word) {
        words = Arrays.asList(word);
        asString = word;
        pattern = Patterns.get(PatternElem.fromWord(word));
    }

    public NGram(
            List<String> words) {
        this.words = words;
        asString = StringUtils.join(words, " ");

        List<PatternElem> patternElems =
                new ArrayList<PatternElem>(words.size());
        for (String word : words) {
            patternElems.add(PatternElem.fromWord(word));
        }
        pattern = Patterns.get(patternElems);
    }

    private NGram(
            List<String> words,
            Pattern pattern) {
        this.words = words;
        asString = StringUtils.join(words, " ");
        this.pattern = pattern;
    }

    private NGram(
            List<String> words,
            String asString,
            Pattern pattern) {
        this.words = words;
        this.asString = asString;
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null || !other.getClass().equals(NGram.class)) {
            return false;
        }

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

    public int size() {
        return words.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public NGram get(int index) {
        return new NGram(words.get(index));
    }

    public boolean isEmptyOrOnlySkips() {
        if (isEmpty()) {
            return true;
        }
        for (String word : words) {
            if (!word.equals(SKP_WORD)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if is either empty or has count greater zero.
     */
    // TODO: move method into class CountCache?
    public boolean seen(CountCache countCache) {
        return isEmpty() || countCache.getAbsolute(this) != 0;
    }

    public NGram concat(String word) {
        List<String> resultWords = new ArrayList<String>(words);
        resultWords.add(word);
        return new NGram(resultWords, asString + " " + word,
                pattern.concat(PatternElem.fromWord(word)));
    }

    public NGram concat(NGram other) {
        List<String> resultWords = new ArrayList<String>(words);
        resultWords.addAll(other.words);
        return new NGram(resultWords);
    }

    public NGram range(int from, int to) {
        if (from < 0 || from > size()) {
            throw new IllegalArgumentException("Illegal from index: " + from);
        } else if (to < 0 || to > size()) {
            throw new IllegalArgumentException("Illegal to index: " + to);
        } else if (from > to) {
            throw new IllegalArgumentException(
                    "From index larger than to index: " + from + " > " + to);
        }

        List<String> resultWords = new ArrayList<String>(to - from);
        List<PatternElem> resultPatternElems =
                new ArrayList<PatternElem>(to - from);

        for (int i = from; i != to; ++i) {
            resultWords.add(words.get(i));
            resultPatternElems.add(pattern.get(i));
        }

        return new NGram(resultWords, Patterns.get(resultPatternElems));
    }

    public NGram replace(String target, String replacement) {
        List<String> resultWords = new ArrayList<String>(size());

        for (String word : words) {
            if (word.equals(target)) {
                resultWords.add(replacement);
            } else {
                resultWords.add(word);
            }
        }

        return new NGram(resultWords);
    }

    public NGram convertSkpToWskp() {
        return replace(SKP_WORD, WSKP_WORD);
    }

    public NGram backoff(BackoffMode backoffMode) {
        if (isEmpty()) {
            throw new IllegalStateException("Can't backoff empty ngrams.");
        }

        // TODO: Rene, is this really correct?
        switch (backoffMode) {
            case SKP:
                List<String> resultWords = new ArrayList<String>(words.size());

                boolean replaced = false;
                for (String word : words) {
                    if (!replaced && !word.equals(SKP_WORD)) {
                        resultWords.add(SKP_WORD);
                        replaced = true;
                    } else {
                        resultWords.add(word);
                    }
                }
                if (!replaced) {
                    throw new IllegalStateException(
                            "Can't backoff ngrams containing only skips.");
                }
                return new NGram(resultWords);

            case DEL:
                return new NGram(words.subList(1, words.size()));

            default:
                throw new IllegalStateException("Unimplemented case in switch.");
        }
    }

    /**
     * Backoffs {@code sequence} at least once, and then until absolute count of
     * it is greater zero. If not possible returns zero. Returned sequence may
     * be empty.
     */
    public NGram
        backoffUntilSeen(BackoffMode backoffMode, CountCache countCache) {
        NGram result = backoff(backoffMode);
        while (!result.seen(countCache)) {
            result = result.backoff(backoffMode);
        }
        return result;
    }

    public Set<NGram> getDifferentiatedNGrams(BackoffMode backoffMode) {
        Set<NGram> result = new LinkedHashSet<NGram>();
        for (int i = 0; i != size(); ++i) {
            if (backoffMode == SKP_AND_DEL || backoffMode == DEL
                    || (backoffMode == DEL_FRONT && i == 0)) {
                List<String> newWords = new LinkedList<String>(words);
                newWords.remove(i);
                result.add(new NGram(newWords));
            }
            if ((backoffMode == SKP || backoffMode == SKP_AND_DEL || (backoffMode == DEL_FRONT && i != 0))
                    && !words.get(i).equals(SKP_WORD)) {
                List<String> newWords = new LinkedList<String>(words);
                newWords.set(i, SKP_WORD);
                result.add(new NGram(newWords));
            }
        }
        return result;
    }

}
