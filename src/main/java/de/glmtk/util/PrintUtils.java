package de.glmtk.util;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

public class PrintUtils {

    public static <K, V >void printMap(Map<K, V> map, String keyValueDelim) {
        printMap(System.out, map, keyValueDelim);
    }

    public static <K, V >void printMap(
            Map<K, V> map,
            String keyValueDelim,
            boolean align) {
        printMap(System.out, map, keyValueDelim, align);
    }

    public static <K, V >void printMap(
            PrintStream out,
            Map<K, V> map,
            String keyValueDelim) {
        printMap(out, map, keyValueDelim, false);
    }

    public static <K, V >void printMap(
            PrintStream out,
            Map<K, V> map,
            String keyValueDelim,
            boolean align) {
        int maxKeyLength = 0;
        if (align) {
            for (K key : map.keySet()) {
                if (key.toString().length() >= maxKeyLength) {
                    maxKeyLength = key.toString().length();
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (Entry<K, V> entry : map.entrySet()) {
            if (align) {
                result.append(String.format("%-" + maxKeyLength + "s", entry
                        .getKey().toString()));
            } else {
                result.append(entry.getKey().toString());
            }
            result.append(keyValueDelim);
            result.append(entry.getValue().toString());
            result.append('\n');
        }
        out.print(result.toString());
    }
}
