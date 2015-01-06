package de.glmtk.util;

public class ObjectUtils {
    /**
     * Same as {@link Object#equals(Object)} but allowing (@code null} for both
     * parameters.
     */
    public static boolean equals(Object lhs,
                                 Object rhs) {
        if (lhs == null) {
            if (rhs == null)
                return true;
            return false;
        }
        if (rhs == null)
            return false;
        return lhs.equals(rhs);
    }

    /**
     * Same as {@link Comparable#compareTo(Object)} but allowing {@code null}
     * for both parameters.
     */
    public static <T extends Comparable<T>> int compare(T lhs,
                                                        T rhs) {
        if (lhs == null) {
            if (rhs == null)
                return 0;
            return 1;
        }
        if (rhs == null)
            return -1;
        return lhs.compareTo(rhs);
    }
}
