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

import static de.glmtk.common.PatternElem.SKP_WORD;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;

public class SequenceCalculator extends Calculator {
    /**
     * If {@link #probMode} = {@link ProbMode#COND}:<br>
     * {@code P(a b c) = P(c | a b) * P(b _ | a) * P (a _ _ | )}
     *
     * <p>
     * If {@link #probMode} = {@link ProbMode#MARG}:<br>
     * {@code P(a b c) = P(c | a b) * P(b | a) * P(a |)}
     */
    @Override
    protected List<SequenceAndHistory> computeQueries(List<String> words) {
        List<SequenceAndHistory> queries = new ArrayList<>();

        List<String> s, h = new ArrayList<>(words);
        for (int i = 0; i != words.size(); ++i) {
            // build s
            s = new ArrayList<>(i + 1);
            s.add(h.get(h.size() - 1));
            if (probMode == ProbMode.COND)
                for (int j = 0; j != i; ++j)
                    s.add(SKP_WORD);

            // build h
            if (h.size() >= 1)
                h = new ArrayList<>(h.subList(0, h.size() - 1));
            else
                h = new ArrayList<>();

            queries.add(new SequenceAndHistory(new NGram(s), new NGram(h)));
        }

        return queries;
    }
}
