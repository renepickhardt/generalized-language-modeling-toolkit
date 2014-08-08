package de.glmtk.smoothing;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.patterns.Pattern;
import de.glmtk.patterns.PatternElem;

public class NGram {

    public static NGram SKIPPED_WORD_NGRAM =
            new NGram(PatternElem.SKIPPED_WORD);

    public final List<String> ngram;

    public NGram() {
        ngram = new ArrayList<String>();
    }

    public NGram(
            String word) {
        ngram = new ArrayList<String>();
        ngram.add(word);
    }

    public NGram(
            List<String> ngram) {
        this.ngram = ngram;
    }

    public int size() {
        return ngram.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns {@code true} if either empty or has count greater zero.
     */
    public boolean seen(Corpus corpus) {
        return isEmpty() || corpus.getAbsolute(this) != 0;
    }

    /**
     * Backoffs {@code sequence} until absolute count of it is greater zero. If
     * not possible returns zero. Returned sequence may be empty.
     */
    public NGram backoffUntilSeen(Corpus corpus) {
        NGram result = this;
        while (!result.seen(corpus)) {
            result = result.backoff();
        }
        return result;
    }

    public NGram concat(String word) {
        List<String> result = new ArrayList<String>(ngram);
        result.add(word);
        return new NGram(result);
    }

    public NGram concat(NGram other) {
        List<String> result = new ArrayList<String>(ngram);
        result.addAll(other.ngram);
        return new NGram(result);
    }

    public NGram backoff() {
        // TODO: Rene: do we add SKPs for deleted words or not?
        return new NGram(ngram.subList(1, ngram.size()));

        //        List<String> result = new ArrayList<String>(ngram.size());
        //        boolean replaced = false;
        //        for (String word : ngram) {
        //            if (!replaced && !word.equals(PatternElem.SKIPPED_WORD)) {
        //                result.add(PatternElem.SKIPPED_WORD);
        //                replaced = true;
        //            } else {
        //                result.add(word);
        //            }
        //        }
        //        return new NGram(result);
    }

    public List<String> toList() {
        return ngram;
    }

    public NGram get(int index) {
        return new NGram(ngram.get(index));
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String word : ngram) {
            if (first) {
                first = false;
            } else {
                result.append(" ");
            }
            result.append(word);
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other == null || other.getClass() != Pattern.class) {
            return false;
        }

        NGram o = (NGram) other;
        return ngram.equals(o.ngram);
    }

    @Override
    public int hashCode() {
        int hash = 25215;
        int mult = 389;

        hash += mult * ngram.hashCode();

        return hash;
    }

}
