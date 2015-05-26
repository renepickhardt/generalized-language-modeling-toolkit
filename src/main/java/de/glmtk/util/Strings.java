package de.glmtk.util;

public class Strings {
    private Strings() {
    }

    public static String requireNotEmpty(String string) {
        if (string.isEmpty())
            throw new IllegalArgumentException("Empty string given.");
        return string;
    }
}
