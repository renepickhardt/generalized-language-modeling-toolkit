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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.glmtk.GlmtkPaths;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Pattern;
import de.glmtk.util.CollectionUtils;

/**
 * Convenience class to construct a {@link Cache} that supports chaining.
 */
public class CacheBuilder {
    private Set<Pattern> counts;
    private boolean ngramTimes;
    private boolean lengthDistribution;
    private Set<String> discounts;
    private Map<String, Set<Pattern>> alphas;
    private Map<String, Set<Pattern>> lambdas;
    private boolean progress;

    public CacheBuilder() {
        counts = new HashSet<>();
        ngramTimes = false;
        lengthDistribution = false;
        discounts = new HashSet<>();
        alphas = new HashMap<>();
        lambdas = new HashMap<>();
        progress = false;
    }

    public Set<Pattern> getCountsPatterns() {
        return counts;
    }

    public CacheBuilder withCounts(Collection<Pattern> counts) {
        this.counts.addAll(counts);
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

    public Set<String> getDiscountModels() {
        return discounts;
    }

    public CacheBuilder withDiscounts(String model) {
        discounts.add(model);
        return this;
    }

    public Map<String, Set<Pattern>> getAlphaModelPatterns() {
        return alphas;
    }

    public CacheBuilder withAlphas(String model,
                                   Collection<Pattern> alphas) {
        Set<Pattern> alphaPatterns = this.alphas.get(model);
        if (alphaPatterns == null) {
            alphaPatterns = new HashSet<>();
            this.alphas.put(model, alphaPatterns);
        }
        alphaPatterns.addAll(alphas);
        return this;
    }

    public Map<String, Set<Pattern>> getLambdaModelPatterns() {
        return lambdas;
    }

    public CacheBuilder withLambdas(String model,
                                    Collection<Pattern> lambdas) {
        Set<Pattern> lambdaPatterns = this.lambdas.get(model);
        if (lambdaPatterns == null) {
            lambdaPatterns = new HashSet<>();
            this.lambdas.put(model, lambdaPatterns);
        }
        lambdaPatterns.addAll(lambdas);
        return this;
    }

    public boolean isWithProgress() {
        return progress;
    }

    public CacheBuilder withProgress() {
        progress = true;
        return this;
    }

    public Set<Pattern> getNeededPatterns() {
        Set<Pattern> result = new HashSet<>();
        result.addAll(counts);
        for (Set<Pattern> alphaPatterns : alphas.values())
            result.addAll(alphaPatterns);
        for (Set<Pattern> lambdaPatterns : lambdas.values())
            result.addAll(lambdaPatterns);
        return result;
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
            total += ngramTimes ? 1 : 0;
            total += lengthDistribution ? 1 : 0;
            total += discounts.size();
            for (Set<Pattern> alphas : this.alphas.values())
                total += alphas.size();
            for (Set<Pattern> lambdas : this.lambdas.values())
                total += lambdas.size();
            cache.setProgress(OUTPUT.newProgress(total));
        }

        if (!counts.isEmpty())
            cache.loadCounts(counts);
        if (ngramTimes)
            cache.loadNGramTimes();
        if (lengthDistribution)
            cache.loadLengthDistribution();
        for (String model : discounts)
            cache.loadDiscounts(model);
        for (Entry<String, Set<Pattern>> entry : alphas.entrySet())
            cache.loadAlphas(entry.getKey(), entry.getValue());
        for (Entry<String, Set<Pattern>> entry : lambdas.entrySet())
            cache.loadLambdas(entry.getKey(), entry.getValue());

        if (progress)
            OUTPUT.endPhases(message + ".");

        return cache;
    }

    public CacheBuilder addAll(CacheBuilder cacheBuilder) {
        counts.addAll(cacheBuilder.counts);
        ngramTimes = ngramTimes || cacheBuilder.ngramTimes;
        lengthDistribution = lengthDistribution
                || cacheBuilder.lengthDistribution;
        discounts.addAll(discounts);
        CollectionUtils.mapAddAll(alphas, cacheBuilder.alphas);
        CollectionUtils.mapAddAll(lambdas, cacheBuilder.lambdas);
        return this;
    }

    @Override
    public String toString() {
        try (Formatter f = new Formatter()) {
            f.format("counts = %s, ", counts);
            f.format("ngramTimes = %b, ", ngramTimes);
            f.format("lengthDistriubtion = %b, ", lengthDistribution);
            f.format("discounts = %s, ", discounts);
            f.format("alphas = %s, ", alphas);
            f.format("lambdas = %s", lambdas);
            return f.toString();
        }
    }
}
