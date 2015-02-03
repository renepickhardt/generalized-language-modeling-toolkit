/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2015 Lukas Schmelzeisen, Rene Pickhardt
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class BinomDiamondTest {
    private static class TestNode extends BinomDiamondNode<TestNode> {
    }

    @Test
    public void testOrder() {
        for (int i = 1; i != 11; ++i)
            assertEquals(i, new BinomDiamond<>(i, TestNode.class).order());
    }

    @Test
    public void testSize() {
        for (int i = 1; i != 11; ++i)
            assertEquals((int) Math.pow(2, i), new BinomDiamond<>(i,
                    TestNode.class).size());
    }

    @Test
    public void testGetTop() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        assertTrue(diamond.getTop().isTop());
        assertFalse(diamond.getBottom().isTop());
    }

    @Test
    public void testGetBottom() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        assertTrue(diamond.getBottom().isBottom());
        assertFalse(diamond.getTop().isBottom());
    }

    @Test
    public void testGet() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        for (TestNode node : diamond)
            assertEquals(node, diamond.get(node.getIndex()));
    }

    @Test
    public void testNumParents() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        assertEquals(0, diamond.getTop().numParents());
        assertEquals(1, diamond.getTop().getChild(0).numParents());

        assertEquals(5, diamond.getBottom().numParents());
        assertEquals(4, diamond.getBottom().getParent(0).numParents());
    }

    @Test
    public void testNumChilds() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        assertEquals(5, diamond.getTop().numChilds());
        assertEquals(4, diamond.getTop().getChild(0).numChilds());

        assertEquals(0, diamond.getBottom().numChilds());
        assertEquals(1, diamond.getBottom().getParent(1).numChilds());
    }

    @Test
    public void testGetChild() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        int numChilds = 0;
        TestNode node = diamond.getTop();
        while (node.numChilds() > 0) {
            node = node.getChild(0);
            ++numChilds;
        }

        assertEquals(diamond.order(), numChilds);
    }

    @Test
    public void testGetParent() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        int numParents = 0;
        TestNode node = diamond.getBottom();
        while (node.numParents() > 0) {
            node = node.getParent(0);
            ++numParents;
        }

        assertEquals(diamond.order(), numParents);
    }

    @Test
    public void testIsAncestor() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(4, TestNode.class);

        TestNode top = diamond.getTop();
        TestNode bottom = diamond.getBottom();
        for (TestNode node : diamond) {
            assertTrue(node.isAncestorOf(node));
            if (node != top)
                assertFalse(node.isAncestorOf(top));
            if (node != bottom)
                assertTrue(node.isAncestorOf(bottom));
        }

        assertFalse(diamond.get(3).isAncestorOf(diamond.get(1)));
        assertFalse(diamond.get(3).isAncestorOf(diamond.get(2)));
        assertFalse(diamond.get(3).isAncestorOf(diamond.get(4)));
        assertFalse(diamond.get(3).isAncestorOf(diamond.get(8)));
        assertTrue(diamond.get(3).isAncestorOf(diamond.get(7)));
        assertTrue(diamond.get(3).isAncestorOf(diamond.get(11)));
        assertFalse(diamond.get(3).isAncestorOf(diamond.get(13)));
        assertFalse(diamond.get(3).isAncestorOf(diamond.get(14)));
    }

    @Test
    public void testIsDescendantOf() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(4, TestNode.class);

        TestNode top = diamond.getTop();
        TestNode bottom = diamond.getBottom();
        for (TestNode node : diamond) {
            assertTrue(node.isDescendantOf(node));
            if (node != top)
                assertTrue(node.isDescendantOf(top));
            if (node != bottom)
                assertFalse(node.isDescendantOf(bottom));
        }

        assertTrue(diamond.get(3).isDescendantOf(diamond.get(1)));
        assertTrue(diamond.get(3).isDescendantOf(diamond.get(2)));
        assertFalse(diamond.get(3).isDescendantOf(diamond.get(4)));
        assertFalse(diamond.get(3).isDescendantOf(diamond.get(8)));
        assertFalse(diamond.get(3).isDescendantOf(diamond.get(7)));
        assertFalse(diamond.get(3).isDescendantOf(diamond.get(11)));
        assertFalse(diamond.get(3).isDescendantOf(diamond.get(13)));
        assertFalse(diamond.get(3).isDescendantOf(diamond.get(14)));
    }

    @Test
    public void testIterateOf() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(5, TestNode.class);

        Iterator<TestNode> iter = diamond.iterator();
        for (int i = 0; i != diamond.size(); ++i) {
            assertTrue(iter.hasNext());
            TestNode node = iter.next();
            assertEquals(i, node.getIndex());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterateInOrder() {
        BinomDiamond<TestNode> diamond = new BinomDiamond<>(4, TestNode.class);

        List<Integer> order = new ArrayList<>(diamond.size());
        for (TestNode testNode : diamond.inOrder())
            order.add(testNode.getIndex());

        System.out.println(order);
        assertEquals(Arrays.asList(0, 1, 2, 4, 8, 3, 5, 6, 9, 10, 12, 7, 11,
                13, 14, 15), order);
    }
}
