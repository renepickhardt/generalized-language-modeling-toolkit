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

public class NGramTimes {
    private long oneCount;
    private long twoCount;
    private long threeCount;
    private long fourCount;

    public NGramTimes() {
        this(0L, 0L, 0L, 0L);
    }

    public NGramTimes(long oneCount,
                      long twoCount,
                      long threeCount,
                      long fourCount) {
        set(oneCount, twoCount, threeCount, fourCount);
    }

    public long getOneCount() {
        return oneCount;
    }

    public long getTwoCount() {
        return twoCount;
    }

    public long getThreeCount() {
        return threeCount;
    }

    public long getFourCount() {
        return fourCount;
    }

    public void setOneCount(long oneCount) {
        this.oneCount = oneCount;
    }

    public void setTwoCount(long twoCount) {
        this.twoCount = twoCount;
    }

    public void setThreeCount(long threeCount) {
        this.threeCount = threeCount;
    }

    public void setFourCount(long fourCount) {
        this.fourCount = fourCount;
    }

    public void set(long oneCount,
                    long twoCount,
                    long threeCount,
                    long fourCount) {
        this.oneCount = oneCount;
        this.twoCount = twoCount;
        this.threeCount = threeCount;
        this.fourCount = fourCount;
    }

    public void add(long count) {
        if (count == 1) {
            ++oneCount;
        } else if (count == 2) {
            ++twoCount;
        } else if (count == 3) {
            ++threeCount;
        } else if (count == 4) {
            ++fourCount;
        }
    }

    @Override
    public String toString() {
        return String.format("1=%d, 2=%d, 3=%d, 4=%d", oneCount, twoCount,
            threeCount, fourCount);
    }
}
