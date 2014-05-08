package de.typology.patterns;

import java.util.HashMap;
import java.util.Map;

public enum PatternElem {

    CNT("1"),

    SKP("0"),

    POS("2"),

    WSKP("x"),

    PSKP("y"),

    WPOS("z"),

    DEL("d");

    public static String SKIPPED_WORD = "_";

    private static Map<String, PatternElem> fromString =
            new HashMap<String, PatternElem>();
    static {
        for (PatternElem elem : PatternElem.values()) {
            fromString.put(elem.toString(), elem);
        }
    }

    private String string;

    private PatternElem(
            String string) {
        this.string = string;
    }

    public String apply(String word, String pos) {
        switch (this) {
            case CNT:
                return word;
            case SKP:
                return SKIPPED_WORD;
            case POS:
                return pos;
            default:
                throw new IllegalStateException(
                        "Illegal PatternElem in PatternElem#apply: \"" + this
                                + "\".");
        }
    }

    @Override
    public String toString() {
        return string;
    }

    public static PatternElem fromString(String elem) {
        return fromString.get(elem);
    }

}
