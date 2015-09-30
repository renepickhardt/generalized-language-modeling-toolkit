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
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        CompletionTrieEntry o = (CompletionTrieEntry) other;
        return string.equals(o.string) && score == o.score;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (score ^ (score >>> 32));
        result = prime * result + string.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CompletionTrieEntry [string=" + getString() + ", score="
            + getScore() + "]";
    }
}
