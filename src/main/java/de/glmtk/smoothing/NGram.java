package de.glmtk.smoothing;

import java.util.ArrayList;
import java.util.List;

public class NGram {

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

    public NGram concat(String word) {
        List<String> newNGram = new ArrayList<String>(ngram);
        newNGram.add(word);
        return new NGram(newNGram);
    }

    public NGram concat(NGram other) {
        List<String> newNGram = new ArrayList<String>(ngram);
        newNGram.addAll(other.ngram);
        return new NGram(newNGram);
    }

    public NGram backoff() {
        // TODO: Rene: do we add SKPs for deleted words or not?
        return new NGram(ngram.subList(1, ngram.size()));
    }

    public List<String> toList() {
        return ngram;
    }

    public NGram firstWord() {
        return new NGram(ngram.subList(0, 1));
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

}
