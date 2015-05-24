package de.glmtk.util.revamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtils {
    public static <T> List<T> list() {
        return new ArrayList<>();
    }

    public static <T> List<T> list(T... values) {
        return Arrays.asList(values);
    }
}
