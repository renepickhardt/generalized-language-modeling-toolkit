package de.glmtk.pattern;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum PatternElem {

    CNT("1"),

    POS("2"),

    SKP("0"),

    WSKP("x"),

    PSKP("y"),

    WPOS("z"),

    DEL("d");

    public static final String SKIPPED_WORD = "_";

    public static final Set<PatternElem> CSKIP_ELEMS =
            new HashSet<PatternElem>();
    static {
        CSKIP_ELEMS.addAll(Arrays.asList(PatternElem.WSKP, PatternElem.PSKP,
                PatternElem.WPOS));
    }

    private static final Map<String, PatternElem> FROM_STRING =
            new HashMap<String, PatternElem>();
    static {
        for (PatternElem elem : PatternElem.values()) {
            FROM_STRING.put(elem.toString(), elem);
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
        return FROM_STRING.get(elem);
    }

}
