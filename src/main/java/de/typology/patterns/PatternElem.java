package de.typology.patterns;

public enum PatternElem {

    CNT,

    SKP,

    POS,

    DEL;

    public String apply(String word, String pos) {
        switch (this) {
            case CNT:
                return word;
            case SKP:
                return "_";
            case POS:
                return pos;
            case DEL:
                return null;
            default:
                throw new IllegalStateException(
                        "Unimplemted PatternElem in PatternElem#apply: \""
                                + this + "\".");
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case CNT:
                return "1";
            case SKP:
                return "_";
            case POS:
                return "p";
            case DEL:
                return "d";
            default:
                throw new IllegalStateException(
                        "Unimplemented PatternElem in PatternElem#toString.");
        }
    }

    public static PatternElem fromString(String elem) {
        switch (elem) {
            case "1":
                return CNT;
            case "_":
                return SKP;
            case "p":
                return POS;
            case "d":
                return DEL;
            default:
                throw new IllegalStateException(
                        "Unimplemented PatternElem in PatternElem#fromString: \""
                                + elem + "\".");
        }
    }

}
