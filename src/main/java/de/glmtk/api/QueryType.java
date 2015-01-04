package de.glmtk.api;

public enum QueryType {
    COND("Cond"),
    SEQUENCE("Sequence"),
    MARKOV("Markov");

    private static final String DELIM = "-";

    public static final String cond() {
        return COND.toString();
    }

    public static final String sequence() {
        return SEQUENCE.toString();
    }

    public static final String markov(int markovOrder) {
        return MARKOV.toString() + DELIM + Integer.toString(markovOrder);
    }

    public static QueryType fromString(String string) {
        if (string.equals(COND.toString()))
            return COND;
        else if (string.equals(SEQUENCE.toString()))
            return SEQUENCE;
        else if (string.startsWith(MARKOV.toString() + DELIM))
            return MARKOV;
        else
            return null;
    }

    public static Integer getMarkovOrder(String string) {
        if (!string.startsWith(MARKOV.toString() + DELIM))
            return null;

        String markovOrderStr = string.substring(MARKOV.toString().length()
                + DELIM.length());
        return Integer.parseInt(markovOrderStr);
    }

    private String string;

    private QueryType(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }
}
