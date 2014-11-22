package de.glmtk.utils;

import static de.glmtk.utils.PatternElem.CNT;
import static de.glmtk.utils.PatternElem.SKIPPED_WORD;
import static de.glmtk.utils.PatternElem.SKP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import de.glmtk.smoothing.CountCache;
import de.glmtk.smoothing.ProbMode;

/**
 * Immutable.
 */
public class NGram {

    // TODO: Test this class;

    public static NGram SKIPPED_NGRAM = new NGram(SKIPPED_WORD);

    private final List<String> words;

    private String asString;

    private Pattern pattern;

    public NGram() {
        words = new ArrayList<String>();
        asString = "";
        pattern = new Pattern();
    }

    public NGram(
            String word) {
        words = Arrays.asList(word);
        asString = word;
        pattern = new Pattern(wordToPatternElem(word));
    }

    public NGram(
            List<String> words) {
        this.words = words;
        asString = StringUtils.join(words, " ");

        List<PatternElem> patternElems =
                new ArrayList<PatternElem>(words.size());
        for (String word : words) {
            patternElems.add(wordToPatternElem(word));
        }
        pattern = new Pattern(patternElems);
    }

    private NGram(
            List<String> words,
            String asString,
            Pattern pattern) {
        this.words = words;
        this.asString = asString;
        this.pattern = pattern;
    }

    private PatternElem wordToPatternElem(String word) {
        return word.equals(SKIPPED_WORD) ? SKP : CNT;
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

    public Pattern toPattern() {
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
            if (!word.equals(SKIPPED_WORD)) {
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
                pattern.concat(wordToPatternElem(word)));
    }

    public NGram concat(NGram other) {
        List<String> resultWords = new ArrayList<String>(words);
        resultWords.addAll(other.words);
        return new NGram(resultWords);
    }

    public NGram backoff(ProbMode probMode) {
        if (isEmpty()) {
            throw new IllegalStateException("Can't backoff empty ngrams.");
        }

        // TODO: Rene, is this really correct?
        switch (probMode) {
            case COND:
                List<String> resultWords = new ArrayList<String>(words.size());

                boolean replaced = false;
                for (String word : words) {
                    if (!replaced && !word.equals(SKIPPED_WORD)) {
                        resultWords.add(SKIPPED_WORD);
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

            case MARG:
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
    public NGram backoffUntilSeen(ProbMode probMode, CountCache countCache) {
        NGram result = backoff(probMode);
        while (!result.seen(countCache)) {
            result = result.backoff(probMode);
        }
        return result;
    }

    public NGram differentiate(int index, ProbMode probMode) {
        if (index < 0 || index >= words.size()) {
            throw new IllegalStateException("Illegal differentiate index.");
        }

        //        if (index == 0) {
        //            return backoff(probMode);
        //        }

        List<String> resultWords = new LinkedList<String>(words);
        resultWords.set(index, SKIPPED_WORD);
        return new NGram(resultWords);
    }

}
