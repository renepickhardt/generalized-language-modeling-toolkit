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
 * Class to construct {@link Cache} instances.
 */
public class CacheBuilder {
    public enum CacheImplementation {
        HASH_MAP,
        COMPLETION_TRIE;
    }

    private CacheImplementation cacheImplementation = CacheImplementation.HASH_MAP;
    private Set<Pattern> counts = new HashSet<>();
    private Set<Pattern> gammas = new HashSet<>();
    private boolean words = false;
    private boolean ngramTimes = false;
    private boolean lengthDistribution = false;
    private boolean progress = false;

    public CacheImplementation getCacheImplementation() {
        return cacheImplementation;
    }

    public CacheBuilder withCacheImplementation(CacheImplementation cacheImplementation) {
        this.cacheImplementation = cacheImplementation;
        return this;
    }

    public Set<Pattern> getCountsPatterns() {
        return counts;
    }

    public CacheBuilder withCounts(Collection<Pattern> counts) {
        this.counts.addAll(counts);
        return this;
    }

    public CacheBuilder withGammas(Collection<Pattern> gammas) {
        withNGramTimes();
        this.gammas.addAll(gammas);
        return this;
    }

    public Set<Pattern> getGammaPatterns() {
        return gammas;
    }

    public boolean isWithWords() {
        return words;
    }

    public CacheBuilder withWords() {
        words = true;
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

        AbstractCache cache = newCacheFromImplementation(paths);
        if (progress) {
            int total = 0;
            total += counts.size();
            total += gammas.size();
            total += words ? 1 : 0;
            total += ngramTimes ? 1 : 0;
            total += lengthDistribution ? 1 : 0;
            cache.setProgress(OUTPUT.newProgress(total));
        }

        if (!counts.isEmpty())
            cache.loadCounts(counts);
        if (!gammas.isEmpty())
            cache.loadGammas(gammas);
        if (words)
            cache.loadWords();
        if (ngramTimes)
            cache.loadNGramTimes();
        if (lengthDistribution)
            cache.loadLengthDistribution();

        if (progress)
            OUTPUT.endPhases(message + ".");

        return cache;
    }

    private AbstractCache newCacheFromImplementation(GlmtkPaths paths) {
        switch (cacheImplementation) {
            case HASH_MAP:
                return new HashMapCache(paths);

            case COMPLETION_TRIE:
                return new CompletionTrieCache(paths);

            default:
                throw new IllegalStateException(
                        "Unimplemented cache implementation: "
                                + cacheImplementation);
        }
    }

    public CacheBuilder addAll(CacheBuilder cacheBuilder) {
        counts.addAll(cacheBuilder.counts);
        gammas.addAll(cacheBuilder.gammas);
        words = words || cacheBuilder.words;
        ngramTimes = ngramTimes || cacheBuilder.ngramTimes;
        lengthDistribution = lengthDistribution
                || cacheBuilder.lengthDistribution;
        return this;
    }

    @Override
    public String toString() {
        try (Formatter f = new Formatter()) {
            f.format("cacheImplementation = %s", cacheImplementation);
            f.format("counts = %s, ", counts);
            f.format("gammas = %s", gammas);
            f.format("words = %s", words);
            f.format("ngramTimes = %b, ", ngramTimes);
            f.format("lengthDistriubtion = %b, ", lengthDistribution);
            f.format("progress = %b", progress);
            return f.toString();
        }
    }
}
