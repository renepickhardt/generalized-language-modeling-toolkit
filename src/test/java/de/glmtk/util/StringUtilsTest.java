/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2015 Lukas Schmelzeisen
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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class StringUtilsTest {
    @Test
    public void testSplit() {
        assertEquals(Arrays.asList("a"), StringUtils.split("a", ' '));
        assertEquals(Arrays.asList("a"), StringUtils.split("   a   ", ' '));
        assertEquals(Arrays.asList("a"), StringUtils.split("a ", ' '));
        assertEquals(Arrays.asList("a"), StringUtils.split(" a", ' '));

        assertEquals(Arrays.asList("a", "a"), StringUtils.split("a a", ' '));
        assertEquals(Arrays.asList("a", "b"), StringUtils.split("a b", ' '));
        assertEquals(Arrays.asList("b", "a"), StringUtils.split("b a", ' '));
        assertEquals(Arrays.asList("a", "a"), StringUtils.split("  a a  ", ' '));
        assertEquals(Arrays.asList("a", "a"),
                StringUtils.split("a    a  ", ' '));

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "e", "f"),
                StringUtils.split("a    b   c d   e e f ", ' '));
    }
}
