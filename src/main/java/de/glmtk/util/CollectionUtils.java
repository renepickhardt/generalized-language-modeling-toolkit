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
}
