/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
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

package de.glmtk.querying.calculator;

import java.util.List;

import de.glmtk.common.NGram;

public class MarkovCalculator extends SequenceCalculator {
    private int markovOrder;

    public MarkovCalculator(int markovOrder) {
        this.markovOrder = markovOrder;
    }

    public void setMarkovOrder(int markovOrder) {
        if (markovOrder <= 0)
            throw new IllegalArgumentException(
                    "Markov markovOrder must be > 0.");
        this.markovOrder = markovOrder;
    }

    @Override
    protected List<SequenceAndHistory> computeQueries(List<String> words) {
        List<SequenceAndHistory> queries = super.computeQueries(words);

        for (int i = 0; i != queries.size(); ++i) {
            SequenceAndHistory query = queries.get(i);
            NGram h = query.history;
            if (h.size() >= markovOrder)
                query.history = h.range(h.size() - markovOrder + 1, h.size());
        }

        return queries;
    }
}
