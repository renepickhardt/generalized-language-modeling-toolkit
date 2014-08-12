package de.glmtk.pattern;

import java.util.HashMap;
import java.util.Map;

public enum PatternElem {

    CNT("1"),

    POS("2"),

    SKP("0"),

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

    public String apply(String word) {
        switch (this) {
            case CNT:
            case POS:
                return word;
            case SKP:
            case WSKP:
            case PSKP:
            case WPOS:
                return SKIPPED_WORD;
            default:
                throw new IllegalStateException(
                        "Illegal PatternElem in PatternElem#apply(String): \""
                                + this + "\".");
        }
    }

    public String apply(String word, String pos) {
        switch (this) {
            case CNT:
                return word;
            case POS:
                return pos;
            case SKP:
            case WSKP:
            case PSKP:
            case WPOS:
                return SKIPPED_WORD;
            default:
                throw new IllegalStateException(
                        "Illegal PatternElem in PatternElem#apply(String, String): \""
                                + this + "\".");
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
