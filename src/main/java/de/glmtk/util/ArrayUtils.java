/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen
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
import java.util.List;

public class ArrayUtils {
    public static void swap(boolean[] array,
                            int i,
                            int j) {
        boolean tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    public static <T> void swap(T[] array,
                                int i,
                                int j) {
        T tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    public static void reverse(boolean[] array,
                               int from,
                               int to) {
        for (int i = from; i != from + (to - from) / 2; ++i)
            swap(array, i, to + from - i - 1);
    }

    public static <T> void reverse(T[] array,
                                   int from,
                                   int to) {
        for (int i = from; i != from + (to - from) / 2; ++i)
            swap(array, i, to + from - i - 1);
    }

    public static <T> List<T> unionWithSingleton(List<T> array,
            T singleton) {
        List<T> union = new ArrayList<>(array.size() + 1);
        union.addAll(array);
        union.add(singleton);
        return union;
    }

    public static <T> List<T> repeat(T elem,
            int times) {
        List<T> array = new ArrayList<>(times);
        for (int i = 0; i != times; ++i)
            array.add(elem);
        return array;
    }
}
