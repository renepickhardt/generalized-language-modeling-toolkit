package de.glmtk.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class to determine average byte consumption of types of objects.
 */
public class StatisticalNumberHelper {
    private static final Logger LOGGER = LogManager.getFormatterLogger(StatisticalNumberHelper.class);

    private static class AverageItem {
        public long max = Long.MIN_VALUE;
        public long min = Long.MAX_VALUE;
        public long number = 0;
        public long count = 0;
    }

    private static Map<String, Long> counters = new HashMap<>();
    private static Map<String, AverageItem> averages = new HashMap<>();

    public static void count(String name) {
        count(name, 1);
    }

    public static void count(String name,
                             long number) {
        if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) {
            Long counter = counters.get(name);
            if (counter == null)
                counter = 0L;
            counter += number;
            counters.put(name, counter);
        }
    }

    public static void average(String name,
                               long number) {
        if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) {
            AverageItem average = averages.get(name);
            if (average == null) {
                average = new AverageItem();
                averages.put(name, average);
            }
            if (number > average.max)
                average.max = number;
            if (number < average.min)
                average.min = number;
            average.number += number;
            ++average.count;
        }
    }

    public static void print() {
        if (LOGGER.getLevel().isLessSpecificThan(Level.DEBUG)) {
            for (Map.Entry<String, Long> entry : counters.entrySet()) {
                String name = entry.getKey();
                Long counter = entry.getValue();
                LOGGER.debug("'%s'-Counter = %d", name, counter);
            }
            for (Map.Entry<String, AverageItem> entry : averages.entrySet()) {
                String name = entry.getKey();
                AverageItem average = entry.getValue();
                LOGGER.debug("'%s'-Average = %.2f (min=%d max=%d)", name,
                        (double) average.number / average.count, average.min,
                        average.max);
            }
        }
    }
}
