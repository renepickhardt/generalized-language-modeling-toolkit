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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class AlphaCounts implements Iterable<Double> {
    public List<Double> alphas;

    public AlphaCounts() {
        alphas = new ArrayList<>();
    }

    public AlphaCounts(Collection<Double> alphas) {
        set(alphas);
    }

    public double get(int index) {
        return alphas.get(index);
    }

    public void set(int index,
                    double alpha) {
        alphas.set(index, alpha);
    }

    public void set(Collection<Double> alphas) {
        this.alphas = new ArrayList<>(alphas);
    }

    public void append(double alpha) {
        alphas.add(alpha);
    }

    public int size() {
        return alphas.size();
    }

    @Override
    public Iterator<Double> iterator() {
        return alphas.iterator();
    }

    @Override
    public String toString() {
        return alphas.toString();
    }
}
