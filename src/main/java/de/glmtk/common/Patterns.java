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

import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.POS;
import static de.glmtk.common.PatternElem.PSKP;
import static de.glmtk.common.PatternElem.WSKP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Patterns {
    private static final Map<String, Pattern> AS_STRING_TO_PATTERN = new HashMap<>();

    public static Pattern get() {
        Pattern pattern = AS_STRING_TO_PATTERN.get("");
        if (pattern == null) {
            pattern = new Pattern(new ArrayList<PatternElem>(), "");
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(PatternElem elem) {
        Pattern pattern = AS_STRING_TO_PATTERN.get(elem.toString());
        if (pattern == null) {
            pattern = new Pattern(Arrays.asList(elem), elem.toString());
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(List<PatternElem> elems) {
        StringBuilder asStringBuilder = new StringBuilder();
        for (PatternElem elem : elems)
            asStringBuilder.append(elem.toString());
        String asString = asStringBuilder.toString();

        Pattern pattern = AS_STRING_TO_PATTERN.get(asString);
        if (pattern == null) {
            pattern = new Pattern(elems, asString);
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(String asString) {
        Pattern pattern = AS_STRING_TO_PATTERN.get(asString);
        if (pattern == null) {
            List<PatternElem> elems = new ArrayList<>(asString.length());
            for (char elemAsChar : asString.toCharArray()) {
                PatternElem elem = PatternElem.fromChar(elemAsChar);
                if (elem == null)
                    throw new IllegalArgumentException(String.format(
                            "Unkown PatternElem: '%s'.", elemAsChar));
                elems.add(elem);
            }

            pattern = new Pattern(elems, asString);
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Set<Pattern> getMany(String... patterns) {
        Set<Pattern> result = new HashSet<>();
        for (String pattern : patterns)
            result.add(Patterns.get(pattern));
        return result;
    }

    private static void cachePattern(Pattern pattern) {
        AS_STRING_TO_PATTERN.put(pattern.toString(), pattern);
    }

    // TODO: while loop is almost certainly wrong
    public static Set<Pattern> getPosPatterns(Set<Pattern> patterns) {
        Set<Pattern> result = new HashSet<>();
        for (Pattern pattern : patterns) {
            Pattern curPattern = pattern, lastPattern = null;
            do {
                curPattern = curPattern.replaceLast(CNT, POS);
                result.add(curPattern);
                result.add(curPattern.replace(WSKP, PSKP));
                lastPattern = curPattern;
            } while (curPattern != lastPattern);
        }
        return result;
    }

    public static Set<Pattern> getCombinations(int modelSize,
                                               List<PatternElem> elems) {
        Set<Pattern> result = new HashSet<>();

        for (int i = 1; i != modelSize + 1; ++i)
            for (int j = 0; j != pow(elems.size(), i); ++j) {
                List<PatternElem> pattern = new ArrayList<>(i);
                int n = j;
                for (int k = 0; k != i; ++k) {
                    pattern.add(elems.get(n % elems.size()));
                    n /= elems.size();
                }
                result.add(Patterns.get(pattern));
            }

        return result;
    }

    /**
     * Faster than {@link Math#pow(double, double)} because this is only for
     * ints.
     */
    private static int pow(int base,
                           int power) {
        int result = 1;
        for (int i = 0; i != power; ++i)
            result *= base;
        return result;
    }

    public static Map<Integer, Set<Pattern>> groupPatternsBySize(Set<Pattern> patterns) {
        Map<Integer, Set<Pattern>> result = new HashMap<>();
        for (Pattern pattern : patterns) {
            Set<Pattern> patternsWithSize = result.get(pattern.size());
            if (patternsWithSize == null) {
                patternsWithSize = new HashSet<>();
                result.put(pattern.size(), patternsWithSize);
            }
            patternsWithSize.add(pattern);
        }
        return result;
    }
}
