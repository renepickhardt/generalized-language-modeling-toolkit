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

package de.glmtk.counts;

public class AlphaCount {
    public double normal;
    public double discounted;

    public AlphaCount() {
        this(0.0, 0.0);
    }

    public AlphaCount(double normal,
                      double discounted) {
        set(normal, discounted);
    }

    public double getNormal() {
        return normal;
    }

    public double getDiscounted() {
        return discounted;
    }

    public void setNormal(double normal) {
        this.normal = normal;
    }

    public void setDiscounted(double discounted) {
        this.discounted = discounted;
    }

    public void set(double normal,
                    double discounted) {
        this.normal = normal;
        this.discounted = discounted;
    }

    @Override
    public String toString() {
        return String.format("normal=%e, discounted=%e", normal, discounted);
    }
}
