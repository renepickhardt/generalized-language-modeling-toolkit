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

public class Discounts {
    private double one;
    private double two;
    private double threePlus;

    public Discounts() {
        this(0L, 0L, 0L);
    }

    public Discounts(double one,
                     double two,
                     double threePlus) {
        set(one, two, threePlus);
    }

    public double getOne() {
        return one;
    }

    public double getTwo() {
        return two;
    }

    public double getThreePlus() {
        return threePlus;
    }

    public double getForCount(long count) {
        if (count == 0)
            return 0.0;
        else if (count == 1)
            return one;
        else if (count == 2)
            return two;
        else
            return threePlus;
    }

    public void setOne(double one) {
        this.one = one;
    }

    public void setTwo(double two) {
        this.two = two;
    }

    public void setThreePlus(double threePlus) {
        this.threePlus = threePlus;
    }

    public void set(double one,
                    double two,
                    double threePlus) {
        this.one = one;
        this.two = two;
        this.threePlus = threePlus;
    }

    @Override
    public String toString() {
        return String.format("[1=%e, 2=%e, 3+=%e]", one, two, threePlus);
    }
}
