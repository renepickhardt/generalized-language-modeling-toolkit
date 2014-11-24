package de.glmtk.utils;

import static de.glmtk.utils.PatternElem.CNT;
import static de.glmtk.utils.PatternElem.POS;
import static de.glmtk.utils.PatternElem.PSKP;
import static de.glmtk.utils.PatternElem.SKP;
import static de.glmtk.utils.PatternElem.WSKP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import de.glmtk.utils.Pattern;
import de.glmtk.utils.PatternElem;

public class PatternTest {

    @Test
    public void testEqual() {
        Pattern a = Pattern.get("101x");
        Pattern b = Pattern.get(Arrays.asList(CNT, SKP, CNT, WSKP));
        assertEquals(a, b);

        Pattern c = Pattern.get("1011");
        assertNotEquals(a, c);
        assertNotEquals(b, c);
    }

    @Test
    public void testCache() {
        Pattern a = Pattern.get("101x");
        Pattern b = Pattern.get(Arrays.asList(CNT, SKP, CNT, WSKP));
        assertTrue(a == b);

        Pattern c = Pattern.get("1011");
        assertFalse(a == c);
        assertFalse(b == c);
    }

    @Test
    public void testToString() {
        Pattern a = Pattern.get("101x");
        Pattern b = Pattern.get(Arrays.asList(CNT, SKP, CNT, WSKP));
        assertEquals(a.toString(), b.toString());
        assertEquals("101x", b.toString());
    }

    @Test
    public void testIterator() {
        Pattern a = Pattern.get("101x");
        int i = -1;
        for (PatternElem elem : a) {
            assertEquals(elem, a.get(++i));
        }
    }

    @Test
    public void testSize() {
        assertEquals(0, Pattern.get().size());
        assertEquals(1, Pattern.get("1").size());
        assertEquals(4, Pattern.get("101x").size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(Pattern.get().isEmpty());
        assertFalse(Pattern.get(CNT).isEmpty());
    }

    @Test
    public void testGet() {
        Pattern a = Pattern.get("101x");
        assertEquals(CNT, a.get(0));
        assertEquals(WSKP, a.get(3));

        try {
            a.get(-1);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            a.get(5);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testContains() {
        Pattern a = Pattern.get("101x");
        assertTrue(a.contains(CNT));
        assertFalse(a.contains(POS));
        assertTrue(a.contains(Arrays.asList(CNT, SKP)));
        assertFalse(a.contains(Arrays.asList(POS, PSKP)));

        try {
            a.contains(new ArrayList<PatternElem>());
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testContainsOnly() {
        Pattern a = Pattern.get("11111");
        assertTrue(a.containsOnly(CNT));
        assertFalse(a.containsOnly(SKP));
        assertTrue(a.containsOnly(Arrays.asList(CNT, SKP)));
        assertFalse(a.containsOnly(Arrays.asList(SKP, POS)));

        try {
            assertFalse(a.containsOnly(new ArrayList<PatternElem>()));
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        Pattern b = Pattern.get("101x");
        assertFalse(b.containsOnly(CNT));
        assertFalse(b.containsOnly(SKP));
        assertFalse(b.containsOnly(Arrays.asList(CNT, SKP)));
        assertTrue(b.containsOnly(Arrays.asList(CNT, SKP, WSKP)));

        Pattern c = Pattern.get();
        assertFalse(c.contains(CNT));
        assertTrue(c.containsOnly(CNT));
        assertFalse(c.contains(Arrays.asList(CNT, SKP)));
        assertTrue(c.containsOnly(Arrays.asList(CNT, SKP)));
    }

    @Test
    public void testIsAbsolute() {
        assertTrue(Pattern.get("1").isAbsolute());
        assertTrue(Pattern.get("0").isAbsolute());
        assertTrue(Pattern.get("10110").isAbsolute());
        assertFalse(Pattern.get("x").isAbsolute());
        assertFalse(Pattern.get("1011x").isAbsolute());
    }

    @Test
    public void testNumElems() {
        Pattern a = Pattern.get("101x");
        assertEquals(2, a.numElems(Arrays.asList(CNT)));
        assertEquals(1, a.numElems(Arrays.asList(SKP)));
        assertEquals(3, a.numElems(Arrays.asList(CNT, SKP)));
        assertEquals(0, a.numElems(Arrays.asList(POS)));

        try {
            assertEquals(a.numElems(new ArrayList<PatternElem>()), 0);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testConcat() {
        Pattern a = Pattern.get("01");
        Pattern b = Pattern.get("011");
        Pattern c = Pattern.get("01011");
        Pattern d = Pattern.get("01101");

        assertEquals(a.concat(CNT), b);
        assertEquals(b.concat(SKP).concat(CNT), d);
        assertEquals(a.concat(b), c);
        assertEquals(b.concat(a), d);
    }

    @Test
    public void testRange() {
        Pattern a = Pattern.get("101x");
        Pattern b = Pattern.get();

        assertEquals(a, a.range(0, a.size()));
        assertEquals(Pattern.get("101"), a.range(0, 3));
        assertEquals(Pattern.get("1"), a.range(0, 1));
        assertEquals(b, a.range(0, 0));
        assertEquals(b, a.range(4, 4));
        assertEquals(b, b.range(0, b.size()));

        try {
            a.range(-1, a.size());
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            a.range(0, a.size() + 1);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testReplace() {
        Pattern a = Pattern.get("101x");
        Pattern b = Pattern.get();

        assertEquals(Pattern.get("111x"), a.replace(SKP, CNT));
        assertEquals(Pattern.get("000x"), a.replace(CNT, SKP));
        assertEquals(a, a.replace(CNT, CNT));
        assertEquals(b, b.replace(CNT, SKP));
    }

    @Test
    public void testReplaceLast() {
        Pattern a = Pattern.get("101x");
        Pattern b = Pattern.get();

        assertEquals(Pattern.get("1011"), a.replaceLast(WSKP, CNT));
        assertEquals(Pattern.get("100x"), a.replaceLast(CNT, SKP));
        assertEquals(Pattern.get("111x"), a.replaceLast(SKP, CNT));
        assertEquals(a, a.replaceLast(CNT, CNT));
        assertEquals(b, b.replaceLast(CNT, SKP));
    }

    @Test
    public void testGetContinuationSource() {
        assertEquals(Pattern.get("1011"), Pattern.get("101x")
                .getContinuationSource());
        assertEquals(Pattern.get("1110"), Pattern.get("1x10")
                .getContinuationSource());
        assertEquals(Pattern.get("1x01"), Pattern.get("1x0x")
                .getContinuationSource());
        assertEquals(Pattern.get("1xy02"), Pattern.get("1xy0y")
                .getContinuationSource());

        try {
            Pattern.get("1011").getContinuationSource();
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }

        try {
            Pattern.get().getContinuationSource();
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
        }
    }
}
