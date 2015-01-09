package de.glmtk.utils;

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

    public static void reverse(boolean[] array,
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

}
