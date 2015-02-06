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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.ListIterator;

public class BinomDiamond<T extends BinomDiamondNode<T>> implements Iterable<T> {
    private T[] diamond;
    private int order;

    public BinomDiamond(int order,
                        Class<T> clazz) {
        if (order <= 0 || order >= 31)
            throw new IllegalArgumentException(
                    "Illegal order, must be: 0 > order > 31.");

        @SuppressWarnings("unchecked")
        T[] diamond = (T[]) Array.newInstance(clazz, 1 << order);
        this.diamond = diamond;
        this.order = order;

        try {
            Constructor<T> cons = clazz.getDeclaredConstructor();
            cons.setAccessible(true);

            for (int index = 0; index != diamond.length; ++index) {
                T node = cons.newInstance();
                node.initBinomDiamondNode(diamond, order, index);
                diamond[index] = node;
            }
        } catch (NoSuchMethodException e1) {
            throw new RuntimeException(
                    "Node class needs constructor without arguments");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Could not invoke node class constructor.");
        }
    }

    public int order() {
        return order;
    }

    public int size() {
        return diamond.length;
    }

    public T getTop() {
        return diamond[0];
    }

    public T getBottom() {
        return diamond[diamond.length - 1];
    }

    public T get(int index) {
        if (index < 0 || index >= diamond.length)
            throw new IllegalArgumentException(
                    "Illegal 'index': needs to be in [0,size[.");
        return diamond[index];
    }

    @Override
    public Iterator<T> iterator() {
        return new GlmDiamondIterator();
    }

    public ListIterator<T> listIterator() {
        return new GlmDiamondIterator();
    }

    public Iterable<T> inOrder() {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new GlmDiamondInOrderIterator();
            }
        };
    }

    private class GlmDiamondIterator implements ListIterator<T> {
        private int position = 0;

        @Override
        public boolean hasNext() {
            return position < diamond.length;
        }

        @Override
        public T next() {
            return diamond[position++];
        }

        @Override
        public boolean hasPrevious() {
            return position > 0;
        }

        @Override
        public T previous() {
            return diamond[--position];
        }

        @Override
        public int nextIndex() {
            return position + 1;
        }

        @Override
        public int previousIndex() {
            return position - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T e) {
            throw new UnsupportedOperationException();
        }
    }

    private class GlmDiamondInOrderIterator implements Iterator<T> {
        private int index = -1;
        private int nextIndex = 0;

        @Override
        public boolean hasNext() {
            return nextIndex != -1;
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * <a href=
         * "http://stackoverflow.com/questions/28310472/generative-function-that-produces-integers-sorted-by-bits-set-then-value/28310788"
         * >Stack Overflow: Generative Function that produces integers sorted by
         * bits set, then value</a>
         */
        @Override
        public T next() {
            index = nextIndex;
            if (nextIndex == 0)
                nextIndex = 1;
            else {
                int x = Integer.bitCount(nextIndex);
                // next integer with x bits set
                int t = (nextIndex | (nextIndex - 1)) + 1;
                nextIndex = t
                        | ((((t & -t) / (nextIndex & -nextIndex)) >> 1) - 1);

                int m = ((1 << x) - 1) << (order - x);
                if (nextIndex > m)
                    nextIndex = (1 << (x + 1)) - 1;
                if (nextIndex > (1 << order) - 1)
                    nextIndex = -1;
            }
            return diamond[index];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
