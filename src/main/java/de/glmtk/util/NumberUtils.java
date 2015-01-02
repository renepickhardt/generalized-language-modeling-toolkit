package de.glmtk.util;

public class NumberUtils {

    public static int clampInt(long value, int min, int max) {
        if (value > max) {
            return max;
        } else if (value < min) {
            return min;
        } else {
            return (int) value;
        }
    }

    public static int clampInt(double value, int min, int max) {
        if (value > max) {
            return max;
        } else if (value < min) {
            return min;
        } else {
            return (int) value;
        }
    }

}
