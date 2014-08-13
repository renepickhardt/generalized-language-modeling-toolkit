package de.glmtk.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class to determine average byte consumption of types of objects.
 */
public class StatisticalNumberHelper {

    private static final Logger LOGGER = LogManager
            .getLogger(StatisticalNumberHelper.class);

    private static class Item {

        public long max = Long.MIN_VALUE;

        public long min = Long.MAX_VALUE;

        public long number = 0;

        public long count = 0;

    }

    private static Map<String, Item> items = new HashMap<String, Item>();

    public static void add(String name, long number) {
        Item item = items.get(name);
        if (item == null) {
            item = new Item();
            items.put(name, item);
        }
        if (number > item.max) {
            item.max = number;
        }
        if (number < item.min) {
            item.min = number;
        }
        item.number += number;
        ++item.count;
    }

    public static void print() {
        for (Map.Entry<String, Item> entry : items.entrySet()) {
            String name = entry.getKey();
            Item item = entry.getValue();
            LOGGER.debug(name + " = " + item.number / item.count + " (min="
                    + item.min + " max=" + item.max + ")");
        }
    }

    public static void reset() {
        items = new HashMap<String, Item>();
    }

}
