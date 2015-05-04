/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen, Rene Pickhardt
 *
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

package de.glmtk.querying.estimator;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.NGram;
import de.glmtk.common.ProbMode;

public interface Estimator {
    @Override
    public String toString();

    public String getName();

    public void setName(String name);

    @Deprecated
    public Cache getCache();

    public void setCache(Cache cache);

    public ProbMode getProbMode();

    public void setProbMode(ProbMode probMode);

    /**
     * Wrapper around {@link #probability(NGram, NGram, int)} to hide recDepth
     * parameter, and to perform error checking.
     */
    public double probability(NGram sequence,
                              NGram history);

    /**
     * This method should only be called from other estimators. All other users
     * probably want to call {@link #probability(NGram, NGram)}.
     *
     * <p>
     * Wrapper around
     * {@link AbstractEstimator#calcProbability(NGram, NGram, int)} to add
     * logging.
     */
    public double probability(NGram sequence,
                              NGram history,
                              int recDepth);

    public CacheBuilder getRequiredCache(int modelSize);
}
