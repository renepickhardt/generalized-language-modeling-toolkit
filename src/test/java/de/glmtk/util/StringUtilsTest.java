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

import static de.glmtk.util.StringUtils.split;
import static de.glmtk.util.StringUtils.splitSparse;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsTest {
    @Test
    public void testSplit() {
        assertEquals(asList("a"), split("a", ' '));
        assertEquals(asList("a"), split("   a   ", ' '));
        assertEquals(asList("a"), split("a ", ' '));
        assertEquals(asList("a"), split(" a", ' '));

        assertEquals(asList("a", "a"), split("a a", ' '));
        assertEquals(asList("a", "b"), split("a b", ' '));
        assertEquals(asList("b", "a"), split("b a", ' '));
        assertEquals(asList("a", "a"), split("  a a  ", ' '));
        assertEquals(asList("a", "a"), split("a    a  ", ' '));

        assertEquals(asList("a", "b", "c", "d", "e", "e", "f"), split(
                "a    b   c d   e e f ", ' '));
    }

    @Test
    public void testsplitSparseSparse() {
        assertEquals(asList("a"), splitSparse("a", ' '));
        assertEquals(asList("", "", "", "a", "", "", ""), splitSparse(
                "   a   ", ' '));
        assertEquals(asList("a", ""), splitSparse("a ", ' '));
        assertEquals(asList("", "a"), splitSparse(" a", ' '));

        assertEquals(asList("a", "a"), splitSparse("a a", ' '));
        assertEquals(asList("a", "b"), splitSparse("a b", ' '));
        assertEquals(asList("b", "a"), splitSparse("b a", ' '));
        assertEquals(asList("", "", "a", "a", "", ""), splitSparse("  a a  ",
                ' '));
        assertEquals(asList("a", "", "", "", "a", "", ""), splitSparse(
                "a    a  ", ' '));

        assertEquals(asList("a", "", "", "", "b", "", "", "c", "d", "", "",
                "e", "e", "f", ""), splitSparse("a    b   c d   e e f ", ' '));
    }
}
