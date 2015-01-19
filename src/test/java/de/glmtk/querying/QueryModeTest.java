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

package de.glmtk.querying;

import static de.glmtk.querying.QueryMode.QueryType.COND;
import static de.glmtk.querying.QueryMode.QueryType.FIXED;
import static de.glmtk.querying.QueryMode.QueryType.MARKOV;
import static de.glmtk.querying.QueryMode.QueryType.SEQUENCE;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Test;

import de.glmtk.querying.QueryMode.QueryType;
import de.glmtk.util.ReflectionUtils;

public class QueryModeTest {
    private static final Field FIELD_TYPE, FIELD_WITH_LENGTH_FREQ, FIELD_ORDER;
    static {
        try {
            FIELD_TYPE = QueryMode.class.getDeclaredField("type");
            FIELD_TYPE.setAccessible(true);
            FIELD_WITH_LENGTH_FREQ = QueryMode.class.getDeclaredField("withLengthFreq");
            FIELD_WITH_LENGTH_FREQ.setAccessible(true);
            FIELD_ORDER = QueryMode.class.getDeclaredField("order");
            FIELD_ORDER.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testForString() throws Exception {
        assertQueryModeEqualsForString("s", SEQUENCE, true, null);
        assertQueryModeEqualsForString("3", FIXED, false, 3);
        assertQueryModeEqualsForString("m2", MARKOV, true, 2);
        assertQueryModeEqualsForString("c5", COND, false, 5);
    }

    private void assertQueryModeEqualsForString(String forString,
                                                QueryType type,
                                                boolean withLengthFreq,
                                                Integer order) throws Exception {
        QueryMode expected = createQueryMode(type, withLengthFreq, order);
        QueryMode actual = QueryMode.forString(forString);
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.isWithLengthFreq(), actual.isWithLengthFreq());
        assertEquals(expected.getOrder(), actual.getOrder());
    }

    private QueryMode createQueryMode(QueryType type,
                                      boolean withLengthFreq,
                                      Integer order) throws Exception {
        QueryMode queryMode = ReflectionUtils.newInstance(QueryMode.class);
        FIELD_TYPE.set(queryMode, type);
        FIELD_WITH_LENGTH_FREQ.set(queryMode, withLengthFreq);
        FIELD_ORDER.set(queryMode, order);
        return queryMode;
    }
}
