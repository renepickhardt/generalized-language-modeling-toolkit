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

package de.glmtk.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionUtils {
    public static <T> boolean equal(Collection<T> lhs,
                                    Collection<T> rhs) {
        List<T> lhsElems = new ArrayList<>(lhs);
        for (T rhsElem : rhs)
            if (!lhsElems.remove(rhsElem))
                return false;
        return lhsElems.size() == 0;
    }

    public static <T> void ensureListSize(List<T> list,
                                          int neededSize,
                                          T defaultValue) {
        int size = list.size();
        if (size <= neededSize)
            for (int i = size; i != neededSize + 1; ++i)
                list.add(defaultValue);
    }

    public static <T> boolean containsAny(Collection<T> haystack,
                                          Collection<T> needles) {
        for (T needle : needles)
            if (haystack.contains(needle))
                return true;
        return false;
    }
}
