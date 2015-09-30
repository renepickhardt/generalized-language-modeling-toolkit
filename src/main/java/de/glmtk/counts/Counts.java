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

package de.glmtk.counts;

/**
 * This class is used for counting continuation counts It is also a wrapper
 * class to handle the continuation counts during Kneser Ney Smoothing. It is
 * thus called during training and also during testing.
 */
public class Counts {
    private long onePlusCount;
    private long oneCount;
    private long twoCount;
    private long threePlusCount;

    public Counts() {
        this(0L, 0L, 0L, 0L);
    }

    public Counts(long onePlusCount,
                  long oneCount,
                  long twoCount,
                  long threePlusCount) {
        set(onePlusCount, oneCount, twoCount, threePlusCount);
    }

    public long getOnePlusCount() {
        return onePlusCount;
    }

    public long getOneCount() {
        return oneCount;
    }

    public long getTwoCount() {
        return twoCount;
    }

    public long getThreePlusCount() {
        return threePlusCount;
    }

    public void setOnePlusCount(long onePlusCount) {
        this.onePlusCount = onePlusCount;
    }

    public void setOneCount(long oneCount) {
        this.oneCount = oneCount;
    }

    public void setTwoCount(long twoCount) {
        this.twoCount = twoCount;
    }

    public void setThreePlusCount(long threePlusCount) {
        this.threePlusCount = threePlusCount;
    }

    public void set(long onePlusCount,
                    long oneCount,
                    long twoCount,
                    long threePlusCount) {
        this.onePlusCount = onePlusCount;
        this.oneCount = oneCount;
        this.twoCount = twoCount;
        this.threePlusCount = threePlusCount;
    }

    public void add(Counts counts) {
        onePlusCount += counts.onePlusCount;
        oneCount += counts.oneCount;
        twoCount += counts.twoCount;
        threePlusCount += counts.threePlusCount;
    }

    public void add(long count) {
        onePlusCount += count;
        if (count == 1) {
            ++oneCount;
        }
        if (count == 2) {
            ++twoCount;
        }
        if (count >= 3) {
            ++threePlusCount;
        }
    }

    public void addOne(long count) {
        ++onePlusCount;
        if (count == 1) {
            ++oneCount;
        } else if (count == 2) {
            ++twoCount;
        } else if (count >= 3) {
            ++threePlusCount;
        }
    }

    @Override
    public String toString() {
        return String.format("1+=%d, 1=%d, 2=%d, 3+=%d", onePlusCount, oneCount,
            twoCount, threePlusCount);
    }

    /**
     * Currently only used in testing.
     *
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Counts o = (Counts) other;
        if (onePlusCount != o.onePlusCount) {
            return false;
        } else if (oneCount != o.oneCount) {
            return false;
        } else if (twoCount != o.twoCount) {
            return false;
        } else if (threePlusCount != o.threePlusCount) {
            return false;
        }
        return true;
    }

    /**
     * Has to be implemented, because {@link #equals(Object)} is implemented.
     *
     * <p>
     * Implementation guided by: <a href=
     * "http://www.angelikalanger.com/Articles/EffectiveJava/03.HashCode/03.HashCode.html"
     * >Angelika Langer: Implementing the hashCode() Method</a>
     *
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 23984;
        int mult = 457;

        hash += mult * onePlusCount;
        hash += mult * oneCount;
        hash += mult * twoCount;
        hash += mult * threePlusCount;

        return hash;
    }
}
