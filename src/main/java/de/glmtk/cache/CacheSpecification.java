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
import de.glmtk.common.PatternElem;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;

/**
 * Class to construct {@link Cache} instances.
 *
 * TODO: Rename to CacheSpecification?
 */
public class CacheSpecification {
    public enum CacheImplementation {
        HASH_MAP,
        COMPLETION_TRIE;
    }

    private CacheImplementation cacheImplementation = CacheImplementation.HASH_MAP;
    private boolean words = false;
    private boolean discounts = false;
    private boolean lengthDistribution = false;
    private Set<Pattern> counts = new HashSet<>();
    private Set<Pattern> gammas = new HashSet<>();
    private boolean progress = false;

    public CacheImplementation getCacheImplementation() {
        return cacheImplementation;
    }

    // TODO: move CacheImplementation setting to #build() parameter.
    public CacheSpecification withCacheImplementation(CacheImplementation cacheImplementation) {
        this.cacheImplementation = cacheImplementation;
        return this;
    }

    public boolean isWithProgress() {
        return progress;
    }

    public CacheSpecification withProgress() {
        progress = true;
        return this;
    }

    public Set<Pattern> getRequiredPatterns() {
        Set<Pattern> requiredPatterns = new HashSet<>();
        if (words)
            requiredPatterns.add(Pattern.CNT_PATTERN);
        requiredPatterns.addAll(getCountPatterns());
        for (Pattern gammaPattern : getGammaPatterns()) {
            requiredPatterns.add(gammaPattern.concat(PatternElem.CNT));
            requiredPatterns.add(gammaPattern.concat(PatternElem.WSKP));
        }
        return requiredPatterns;
    }

    // TODO: move to be glmtk method.
    public Cache build(GlmtkPaths paths) throws IOException {
        String message = "Loading data into cache";
        if (progress) {
            OUTPUT.beginPhases(message + "...");
            OUTPUT.setPhase(Phase.LOADING_CACHE);
        }

        AbstractCache cache = newCacheFromImplementation(paths);
        if (progress) {
            int total = 0;
            total += words ? 1 : 0;
            total += discounts ? 1 : 0;
            total += lengthDistribution ? 1 : 0;
            total += counts.size();
            total += gammas.size();
            cache.setProgress(OUTPUT.newProgress(total));
        }

        if (words)
            cache.loadWords();
        if (discounts)
            cache.loadDiscounts();
        if (lengthDistribution)
            cache.loadLengthDistribution();
        if (!counts.isEmpty())
            cache.loadCounts(counts);
        if (!gammas.isEmpty())
            cache.loadGammas(gammas);

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
                throw new SwitchCaseNotImplementedException();
        }
    }

    public CacheSpecification addAll(CacheSpecification cacheBuilder) {
        words = words || cacheBuilder.words;
        discounts = discounts || cacheBuilder.discounts;
        lengthDistribution = lengthDistribution
                || cacheBuilder.lengthDistribution;
        counts.addAll(cacheBuilder.counts);
        gammas.addAll(cacheBuilder.gammas);
        return this;
    }

    @Override
    public String toString() {
        try (Formatter f = new Formatter()) {
            f.format("cacheImplementation = %s", cacheImplementation);
            f.format("progress = %b", progress);
            f.format("words = %s", words);
            f.format("discounts = %b, ", discounts);
            f.format("lengthDistriubtion = %b, ", lengthDistribution);
            f.format("counts = %s, ", counts);
            f.format("gammas = %s", gammas);
            return f.toString();
        }
    }

    // Words ///////////////////////////////////////////////////////////////////

    public boolean isWithWords() {
        return words;
    }

    public CacheSpecification withWords() {
        words = true;
        return this;
    }

    // Discounts ///////////////////////////////////////////////////////////////

    public boolean isWithDiscounts() {
        return discounts;
    }

    public CacheSpecification withDiscounts() {
        discounts = true;
        return this;
    }

    // LengthDistribution //////////////////////////////////////////////////////

    public boolean isWithLengthDistribution() {
        return lengthDistribution;
    }

    public CacheSpecification withLengthDistribution() {
        lengthDistribution = true;
        return this;
    }

    // Counts //////////////////////////////////////////////////////////////////

    public Set<Pattern> getCountPatterns() {
        return counts;
    }

    public CacheSpecification withCounts(Collection<Pattern> counts) {
        this.counts.addAll(counts);
        return this;
    }

    // Gammas //////////////////////////////////////////////////////////////////

    public Set<Pattern> getGammaPatterns() {
        return gammas;
    }

    public CacheSpecification withGammas(Collection<Pattern> gammas) {
        withDiscounts();
        this.gammas.addAll(gammas);
        return this;
    }
}
