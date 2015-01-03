package de.glmtk.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionUtils {
    public static <T> boolean equal(Collection<T> lhs,
                                    Collection<T> rhs) {
        List<T> lhsElems = new ArrayList<T>(lhs);
        for (T rhsElem : rhs)
            if (!lhsElems.remove(rhsElem))
                return false;
        return lhsElems.size() == 0;
    }
}
