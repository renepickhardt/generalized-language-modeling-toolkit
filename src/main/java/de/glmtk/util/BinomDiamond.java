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
    /**
     * For {@link #inOrder()} resp. {@link GlmDiamondInOrderIterator}.
     */
    private int[] widths;

    public BinomDiamond(int order,
                        Class<T> clazz) {
        if (order <= 0 || order >= 31)
            throw new IllegalArgumentException(
                    "Illegal order, must be: 0 > order > 31.");

        @SuppressWarnings("unchecked")
        T[] diamond = (T[]) Array.newInstance(clazz, 1 << order);
        this.diamond = diamond;
        this.order = order;

        widths = new int[order + 1];
        int binomial = 1;
        for (int i = 0; i != order + 1; ++i) {
            widths[i] = binomial;
            binomial *= (order - i);
            binomial /= (i + 1);
        }

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

    private class GlmDiamondInOrderIterator implements ListIterator<T> {
        private int level = 0;
        private int position = 0;

        private int getIndex(int level,
                             int position) {
            int index = (1 << level) - 1;
            for (int i = 0; i != position; ++i) {
                int t = (index | (index - 1)) + 1;
                index = t | ((((t & -t) / (index & -index)) >> 1) - 1);
            }
            return index;
        }

        @Override
        public boolean hasNext() {
            return level <= order;
        }

        @Override
        public T next() {
            int index = getIndex(level, position);
            ++position;
            if (position == widths[level]) {
                ++level;
                position = 0;
            }
            return diamond[index];
        }

        @Override
        public boolean hasPrevious() {
            return level >= 0;
        }

        @Override
        public T previous() {
            int index = getIndex(level, position);
            --position;
            if (position == -1) {
                --level;
                position = widths[level] - 1;
            }
            return diamond[index];
        }

        @Override
        public int nextIndex() {
            int l = level;
            int p = position + 1;
            if (p == widths[level]) {
                ++l;
                p = 0;
            }
            return getIndex(l, p);
        }

        @Override
        public int previousIndex() {
            int l = level;
            int p = position - 1;
            if (p == -1) {
                --l;
                p = widths[l] - 1;
            }
            return getIndex(l, p);
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
}
