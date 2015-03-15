package de.glmtk.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface PeekableIterator<E> extends Iterator<E> {
    @SuppressWarnings("rawtypes")
    public static final PeekableIterator EMPTY_PEEKABLE_ITERATOR = new PeekableIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object peek() {
            throw new NoSuchElementException();
        }
    };

    E peek();
}
