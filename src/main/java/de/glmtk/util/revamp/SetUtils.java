package de.glmtk.util.revamp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SetUtils {
    public static <T> Set<T> set() {
        return new HashSet<>();
    }

    @SafeVarargs
    public static <T> Set<T> set(T... values) {
        Set<T> set = set();
        Collections.addAll(set, values);
        return set;
    }
}
