package de.typology.patterns;

public enum PatternType {

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
                        "Unimplemented PatternType in PatternType#toString.");
        }
    }

    public static PatternType fromString(String type) {
        switch (type) {
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
                        "Unimplemented PatternType in PatternType#fromString: \""
                                + type + "\".");
        }
    }
}
