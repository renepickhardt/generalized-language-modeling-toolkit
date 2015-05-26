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

public class NumberUtils {
    private NumberUtils() {
    }

    public static int clampInt(long value,
                               int min,
                               int max) {
        if (value > max)
            return max;
        else if (value < min)
            return min;
        else
            return (int) value;
    }

    public static int clampInt(double value,
                               int min,
                               int max) {
        if (value > max)
            return max;
        else if (value < min)
            return min;
        else
            return (int) value;
    }
}
