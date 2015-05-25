package de.glmtk.util;

import java.util.Map;

public class Maps {
    public static <T> int maxKeyLength(Map<String, T> map) {
        int maxKeyLength = 0;
        for (String key : map.keySet())
            if (maxKeyLength < key.length())
                maxKeyLength = key.length();
        return maxKeyLength;
    }
}
