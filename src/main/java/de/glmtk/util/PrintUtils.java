/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
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

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

public class PrintUtils {
    private PrintUtils() {
    }

    public static <K, V> void printMap(Map<K, V> map,
                                       String keyValueDelim) {
        printMap(System.out, map, keyValueDelim);
    }

    public static <K, V> void printMap(Map<K, V> map,
                                       String keyValueDelim,
                                       boolean align) {
        printMap(System.out, map, keyValueDelim, align);
    }

    public static <K, V> void printMap(PrintStream out,
                                       Map<K, V> map,
                                       String keyValueDelim) {
        printMap(out, map, keyValueDelim, false);
    }

    public static <K, V> void printMap(PrintStream out,
                                       Map<K, V> map,
                                       String keyValueDelim,
                                       boolean align) {
        int maxKeyLength = 0;
        if (align)
            for (K key : map.keySet())
                if (key.toString().length() >= maxKeyLength)
                    maxKeyLength = key.toString().length();

        StringBuilder result = new StringBuilder();
        for (Entry<K, V> entry : map.entrySet()) {
            if (align)
                result.append(String.format("%-" + maxKeyLength + "s",
                        entry.getKey().toString()));
            else
                result.append(entry.getKey().toString());
            result.append(keyValueDelim);
            result.append(entry.getValue().toString());
            result.append('\n');
        }
        out.print(result.toString());
    }

    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount(bytes, false);
    }

    /**
     * See <a href="http://stackoverflow.com/a/3758880/211404">Stack Overflow:
     * How to convert byte size into human readable format in java?</a>
     */
    public static String humanReadableByteCount(long bytes,
                                                boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
                + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String logHeader(String title) {
        return title + StringUtils.repeat("-", 80 - title.length());
    }
}
