package de.typology.patterns;

public enum PatternElem {

    CNT,

    SKP,

    POS,

    DEL;

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
