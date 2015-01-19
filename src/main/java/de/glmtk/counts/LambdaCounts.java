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

public class LambdaCounts implements Iterable<Double> {
    public List<Double> lambdas;

    public LambdaCounts() {
        lambdas = new ArrayList<>();
    }

    public LambdaCounts(Collection<Double> lambdas) {
        set(lambdas);
    }

    public double get(int index) {
        return lambdas.get(index);
    }

    public void set(int index,
                    double lambda) {
        lambdas.set(index, lambda);
    }

    public void set(Collection<Double> lambdas) {
        this.lambdas = new ArrayList<>(lambdas);
    }

    public void add(double lambda) {
        lambdas.add(lambda);
    }

    public int size() {
        return lambdas.size();
    }

    @Override
    public Iterator<Double> iterator() {
        return lambdas.iterator();
    }
}
