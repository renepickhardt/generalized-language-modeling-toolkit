/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.glmtk.exceptions.SwitchCaseNotImplementedException;


public enum PatternElem {
    CNT('1'), POS('2'), SKP('0'), WSKP('x'), PSKP('y'), WPOS('z'), DEL('d');

    public static final String SKP_WORD = "_";
    public static final String WSKP_WORD = "%";
    public static final Set<PatternElem> CSKIP_ELEMS =
        new HashSet<>(Arrays.asList(WSKP, PSKP, WPOS));
    private static final Map<Character, PatternElem> CHAR_TO_ELEM =
        new HashMap<>();

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

    private char asChar;

    private PatternElem(char asChar) {
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
            case DEL:
            default:
                throw new SwitchCaseNotImplementedException();
        }
    }

    public String apply(String word,
                        String pos) {
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
            case DEL:
            default:
                throw new SwitchCaseNotImplementedException();

        }
    }
}
