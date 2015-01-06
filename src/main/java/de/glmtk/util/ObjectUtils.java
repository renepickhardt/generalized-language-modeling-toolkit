package de.glmtk.util;

public class ObjectUtils {
    public static boolean equals(Object lhs,
                                 Object rhs) {
        if (lhs == null)
            if (rhs == null)
                return true;
            else
                return false;
        else if (rhs == null)
            return false;
        return lhs.equals(rhs);
    }
}
