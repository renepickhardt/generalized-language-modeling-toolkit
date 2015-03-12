package de.glmtk.util.completiontrie;


public class CompletionTrieEntry {
    private String string;
    private long score;

    public CompletionTrieEntry(String string,
                               long score) {
        this.string = string;
        this.score = score;
    }

    public String getString() {
        return string;
    }

    public long getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "CompletionTrieEntry [string=" + getString() + ", score="
                + getScore() + "]";
    }

}
