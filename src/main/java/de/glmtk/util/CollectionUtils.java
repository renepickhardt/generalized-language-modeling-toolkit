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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static <T, U> U getFromNestedMap(Map<T, U> map,
                                            T key1,
                                            String nullError,
                                            String error1) throws IllegalStateException {
        if (map == null) {
            if (nullError != null)
                throw new IllegalStateException(nullError);
            return null;
        }

        U value = map.get(key1);
        if (value == null && error1 != null)
            throw new IllegalStateException(String.format(error1, key1));

        return value;
    }

    public static <T, U, V> V getFromNestedMap(Map<T, Map<U, V>> map,
                                               T key1,
                                               U key2,
                                               String nullError,
                                               String error1,
                                               String error2) {
        Map<U, V> nestedMap = getFromNestedMap(map, key1, nullError, error1);
        if (nestedMap == null)
            return null;

        V value = nestedMap.get(key2);
        if (value == null && error2 != null)
            throw new IllegalStateException(String.format(error2, key1, key2));

        return value;
    }

    public static <T, U, V, W> W getFromNestedMap(Map<T, Map<U, Map<V, W>>> map,
                                                  T key1,
                                                  U key2,
                                                  V key3,
                                                  String nullError,
                                                  String error1,
                                                  String error2,
                                                  String error3) {
        Map<V, W> nestedMap = getFromNestedMap(map, key1, key2, nullError,
                error1, error2);
        if (nestedMap == null)
            return null;

        W value = nestedMap.get(key3);
        if (value == null && error3 != null)
            throw new IllegalStateException(String.format(error3, key1, key2,
                    key3));

        return value;
    }

    public static <T, U> void putIntoNestedMap(Map<T, U> map,
                                               T key1,
                                               U value) {
        map.put(key1, value);
    }

    public static <T, U, V> void putIntoNestedMap(Map<T, Map<U, V>> map,
                                                  T key1,
                                                  U key2,
                                                  V value) {
        Map<U, V> nestedMap = map.get(key1);
        if (nestedMap == null) {
            nestedMap = new HashMap<>();
            map.put(key1, nestedMap);
        }
        putIntoNestedMap(nestedMap, key2, value);
    }

    public static <T, U, V, W> void putIntoNestedMap(Map<T, Map<U, Map<V, W>>> map,
                                                     T key1,
                                                     U key2,
                                                     V key3,
                                                     W value) {
        Map<U, Map<V, W>> nestedMap = map.get(key1);
        if (nestedMap == null) {
            nestedMap = new HashMap<>();
            map.put(key1, nestedMap);
        }
        putIntoNestedMap(nestedMap, key2, key3, value);
    }
}
