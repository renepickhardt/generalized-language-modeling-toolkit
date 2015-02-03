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

public class BinomDiamondNode<T extends BinomDiamondNode<T>> {
    private T[] diamond;
    private int order;
    private int index;

    /* package */final void initBinomDiamondNode(T[] diamond,
                                                 int order,
                                                 int index) {
        if (this.diamond != null)
            throw new IllegalStateException(
                    "BinomDiamondNode already initalized.");
        this.diamond = diamond;
        this.order = order;
        this.index = index;
    }

    public final int getIndex() {
        return index;
    }

    public final int getOrder() {
        return order;
    }

    public final int getLevel() {
        return Integer.bitCount(index);
    }

    public final boolean isTop() {
        return index == 0;
    }

    public final boolean isBottom() {
        return index == diamond.length - 1;
    }

    public final int numParents() {
        return Integer.bitCount(index);
    }

    public final int numChilds() {
        return order - Integer.bitCount(index);
    }

    public final T getParent(int num) {
        if (num < 0)
            throw new IllegalArgumentException("Num must not be negative.");
        int n = -1;
        for (int i = 0; i != order; ++i) {
            int childIndex = index & ~(1 << i);
            if (childIndex == index)
                continue;

            ++n;
            if (n == num)
                return diamond[childIndex];
        }
        throw new IllegalArgumentException("Node does not have so many childs.");
    }

    public final T getChild(int num) {
        if (num < 0)
            throw new IllegalArgumentException("Num must not be negative.");
        int n = -1;
        for (int i = 0; i != order; ++i) {
            int parentIndex = index | (1 << i);
            if (parentIndex == index)
                continue;

            ++n;
            if (n == num)
                return diamond[parentIndex];
        }
        throw new IllegalArgumentException(
                "Node does not have so many parents.");
    }

    public final boolean isAncestorOf(T node) {
        return (index & node.getIndex()) == index;
    }

    public final boolean isDescendantOf(T node) {
        return (index | node.getIndex()) == index;
    }
}
