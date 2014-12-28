package de.glmtk.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum PatternElem {

    CNT('1'),

    POS('2'),

    SKP('0'),

    WSKP('x'),

    PSKP('y'),

    WPOS('z'),

    DEL('d');

    private char asChar;

    private PatternElem(
            char asChar) {
        this.asChar = asChar;
    }

    @Override
    public String toString() {
        return Character.toString(asChar);
    }

    public char toChar() {
        return asChar;
    }

    public String apply(String word) {
        switch (this) {
            case CNT:
                // fallthrough
            case POS:
                return word;
            case SKP:
                return SKP_WORD;
            case WSKP:
                return WSKP_WORD;
            case PSKP:
            case WPOS:

            default:
                throw new IllegalStateException("Unimplemented case in switch.");
        }
    }

    public String apply(String word, String pos) {
        switch (this) {
            case CNT:
                return word;
            case POS:
                return pos;
            case SKP:
                return SKP_WORD;
            case WSKP:
                return WSKP_WORD;
            case PSKP:
            case WPOS:

            default:
                throw new IllegalStateException("Unimplemented case in switch.");

        }
    }

    public static final String SKP_WORD = "_";

    public static final String WSKP_WORD = "%";

    public static final Set<PatternElem> CSKIP_ELEMS =
            new HashSet<PatternElem>(Arrays.asList(WSKP, PSKP, WPOS));

    private static final Map<Character, PatternElem> CHAR_TO_ELEM =
            new HashMap<Character, PatternElem>();
    static {
        for (PatternElem elem : values()) {
            CHAR_TO_ELEM.put(elem.asChar, elem);
        }
    }

    /**
     * Returns {@code null} on fail.
     */
    public static PatternElem fromChar(char elem) {
        return CHAR_TO_ELEM.get(elem);
    }

    public static PatternElem fromWord(String word) {
        if (word.equals(SKP_WORD)) {
            return SKP;
        } else if (word.equals(WSKP_WORD)) {
            return WSKP;
        } else {
            return CNT;
        }
    }

}
