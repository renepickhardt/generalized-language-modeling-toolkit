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

package de.glmtk.cache;

import java.util.SortedSet;

import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.counts.Discounts;
import de.glmtk.counts.NGramTimes;

public interface Cache {
    // Words ///////////////////////////////////////////////////////////////////

    public SortedSet<String> getWords();

    // Discounts ///////////////////////////////////////////////////////////////

    public double getDiscount(NGram ngram);

    public Discounts getDiscounts(Pattern pattern);

    public NGramTimes getNGramTimes(Pattern pattern);

    // LengthDistribution //////////////////////////////////////////////////////

    public double getLengthFrequency(int length);

    public int getMaxSequenceLength();

    // Counts //////////////////////////////////////////////////////////////////

    public long getCount(NGram ngram);

    public long getNumWords();

    public long getVocabSize();

    // Gammas //////////////////////////////////////////////////////////////////

    /**
     * Interpolation weight gamma of the highest order.
     */
    public double getGammaHigh(NGram ngram);

    /**
     * Interpolation weight gamma of all lower orders.
     */
    public double getGammaLow(NGram ngram);
}
