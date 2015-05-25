package de.glmtk.util.revamp;

import java.util.HashMap;
import java.util.Map;

public class MapUtils {
    public static <K, V> Map<K, V> map() {
        return new HashMap<>();
    }

    @SafeVarargs
    public static <K, V> Map<K, V> map(Pair<K, V>... values) {
        Map<K, V> map = map();
        for (Pair<K, V> pair : values)
            map.put(pair.left, pair.right);
        return map;

    }
}
