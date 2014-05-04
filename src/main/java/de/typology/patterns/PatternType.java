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
                return "0";
            case POS:
                return "p";
            case DEL:
                return "d";
            default:
                throw new IllegalStateException(
                        "Unimplemented PatternType in PatternType#toString.");
        }
    }

}
