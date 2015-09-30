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

package de.glmtk.querying.calculator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.common.ProbMode;


public class CondCalculatorTest {
    @Test
    public void testComputQueries() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        CondCalculator calculator = new CondCalculator();
        calculator.setProbMode(ProbMode.MARG);

        Method computeQueries = CondCalculator.class
            .getDeclaredMethod("computeQueries", List.class);
        computeQueries.setAccessible(true);

        // TODO: Write actual test.
        System.out.println(computeQueries.invoke(calculator,
            Arrays.asList("a", "b", "c", "d", "e", "f", "g")));
    }
}
