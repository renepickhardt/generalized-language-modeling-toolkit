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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <a href=
 * "http://stackoverflow.com/questions/221525/how-would-you-implement-an-lru-cache-in-java-6"
 * >Stack Overflow: How would you implement an LRU cache in Java 6?</a>
 */
public class LruHashMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1685593901361902939L;

    private int maxEntries;

    public LruHashMap(int maxEntries) {
        super(maxEntries + 1, 1.0f, true);
        this.maxEntries = maxEntries;
    }

    /**
     * Returns <tt>true</tt> if this <code>LruCache</code> has more entries than
     * the maximum specified when it was created.
     *
     * <p>
     * This method <em>does not</em> modify the underlying <code>Map</code>; it
     * relies on the implementation of <code>LinkedHashMap</code> to do that,
     * but that behavior is documented in the JavaDoc for
     * <code>LinkedHashMap</code>.
     * </p>
     *
     * @param eldest
     *            the <code>Entry</code> in question; this implementation
     *            doesn't care what it is, since the implementation is only
     *            dependent on the size of the cache
     * @return <tt>true</tt> if the oldest
     * @see java.util.LinkedHashMap#removeEldestEntry(Map.Entry)
     */
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > maxEntries;
    }
}
