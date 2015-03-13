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

import static de.glmtk.common.Output.OUTPUT;

import java.io.IOException;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;

import de.glmtk.GlmtkPaths;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Pattern;

/**
 * Convenience class to construct a {@link Cache} that supports chaining.
 */
public class CacheBuilder {
    private Set<Pattern> counts;
    private Set<Pattern> completionCounts;
    private boolean ngramTimes;
    private boolean lengthDistribution;
    private boolean progress;

    public CacheBuilder() {
        counts = new HashSet<>();
        completionCounts = new HashSet<>();
        ngramTimes = false;
        lengthDistribution = false;
        progress = false;
    }

    public Set<Pattern> getCountsPatterns() {
        return counts;
    }

    public CacheBuilder withCounts(Collection<Pattern> counts) {
        this.counts.addAll(counts);
        return this;
    }

    public Set<Pattern> getCompletionCountsPatterns() {
        return completionCounts;
    }

    public CacheBuilder withCompletionCounts(Collection<Pattern> completionCounts) {
        this.completionCounts.addAll(completionCounts);
        return this;
    }

    public boolean isWithNGramTimes() {
        return ngramTimes;
    }

    public CacheBuilder withNGramTimes() {
        ngramTimes = true;
        return this;
    }

    public boolean isWithLengthDistribution() {
        return lengthDistribution;
    }

    public CacheBuilder withLengthDistribution() {
        lengthDistribution = true;
        return this;
    }

    public boolean isWithProgress() {
        return progress;
    }

    public CacheBuilder withProgress() {
        progress = true;
        return this;
    }

    public Cache build(GlmtkPaths paths) throws IOException {
        String message = "Loading data into cache";
        if (progress) {
            OUTPUT.beginPhases(message + "...");
            OUTPUT.setPhase(Phase.LOADING_CACHE);
        }

        Cache cache = new Cache(paths);
        if (progress) {
            int total = 0;
            total += counts.size();
            total += completionCounts.size();
            total += ngramTimes ? 1 : 0;
            total += lengthDistribution ? 1 : 0;
            cache.setProgress(OUTPUT.newProgress(total));
        }

        if (!counts.isEmpty())
            cache.loadCounts(counts);
        if (!completionCounts.isEmpty())
            cache.loadCompletionCounts(completionCounts);
        if (ngramTimes)
            cache.loadNGramTimes();
        if (lengthDistribution)
            cache.loadLengthDistribution();

        if (progress)
            OUTPUT.endPhases(message + ".");

        return cache;
    }

    public CacheBuilder addAll(CacheBuilder cacheBuilder) {
        counts.addAll(cacheBuilder.counts);
        completionCounts.addAll(cacheBuilder.completionCounts);
        ngramTimes = ngramTimes || cacheBuilder.ngramTimes;
        lengthDistribution = lengthDistribution
                || cacheBuilder.lengthDistribution;
        return this;
    }

    @Override
    public String toString() {
        try (Formatter f = new Formatter()) {
            f.format("counts = %s, ", counts);
            f.format("completionCounts = %s", completionCounts);
            f.format("ngramTimes = %b, ", ngramTimes);
            f.format("lengthDistriubtion = %b, ", lengthDistribution);
            f.format("progress = %b", progress);
            return f.toString();
        }
    }
}
