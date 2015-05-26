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

import java.util.HashMap;
import java.util.Map;

import de.glmtk.logging.Logger;

/**
 * Helper class to determine average byte consumption of types of objects.
 */
public class StatisticalNumberHelper {
    private StatisticalNumberHelper() {
    }

    private static final Logger LOGGER = Logger.get(StatisticalNumberHelper.class);

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
        if (LOGGER.isDebugEnabled()) {
            Long counter = counters.get(name);
            if (counter == null)
                counter = 0L;
            counter += number;
            counters.put(name, counter);
        }
    }

    public static void average(String name,
                               long number) {
        if (LOGGER.isDebugEnabled()) {
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
        if (LOGGER.isDebugEnabled()) {
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
