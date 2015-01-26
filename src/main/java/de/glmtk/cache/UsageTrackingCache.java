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

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.counts.AlphaCounts;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discount;
import de.glmtk.counts.LambdaCounts;
import de.glmtk.counts.NGramTimes;

/**
 * Cache that returns random values, but tracks its usage, with the ability to
 * create an actual Cache for all used operations.
 */
public class UsageTrackingCache extends Cache {
    private CacheBuilder cacheBuilder;
    private Random random;

    public UsageTrackingCache() {
        super(null);
        cacheBuilder = new CacheBuilder();
        random = new Random();
    }

    public CacheBuilder toCacheBuilder() {
        return cacheBuilder;
    }

    @Override
    public void loadCounts(Set<Pattern> patterns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadNGramTimes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadLengthDistribution() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadDiscounts(String model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadAlphas(String model,
                           Set<Pattern> patterns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadLambdas(String model,
                            Set<Pattern> patterns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAbsolute(NGram ngram) {
        cacheBuilder.withCounts(Arrays.asList(ngram.getPattern()));

        if (ngram.isEmptyOrOnlySkips())
            return random.nextInt(10) + 1;
        return random.nextInt(11);
    }

    @Override
    public Counts getContinuation(NGram ngram) {
        cacheBuilder.withCounts(Arrays.asList(ngram.getPattern()));

        if (ngram.isEmptyOrOnlySkips())
            return new Counts(random.nextInt(10) + 1, random.nextInt(10) + 1,
                    random.nextInt(10) + 1, random.nextInt(10) + 1);
        return new Counts(random.nextInt(11), random.nextInt(11),
                random.nextInt(11), random.nextInt(11));
    }

    @Override
    public SortedSet<String> getWords() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NGramTimes getNGramTimes(Pattern pattern) {
        cacheBuilder.withNGramTimes();
        return new NGramTimes(random.nextInt(10) + 1, random.nextInt(10) + 1,
                random.nextInt(10) + 1, random.nextInt(10) + 1);
    }

    @Override
    public double getLengthFrequency(int length) {
        cacheBuilder.withLengthDistribution();
        return random.nextDouble();
    }

    @Override
    public int getMaxSequenceLength() {
        cacheBuilder.withLengthDistribution();
        return random.nextInt(100);
    }

    @Override
    public Discount getDiscount(String model,
                                Pattern pattern) {
        cacheBuilder.withDiscounts(model);
        return new Discount(random.nextDouble(), random.nextDouble(),
                random.nextDouble());
    }

    @Override
    public AlphaCounts getAlpha(String model,
                                NGram ngram) {
        cacheBuilder.withAlphas(model, Arrays.asList(ngram.getPattern()));
        AlphaCounts result = new AlphaCounts();
        for (int i = 0; i != ngram.size() + 1; ++i)
            result.append(random.nextDouble());
        return result;
    }

    @Override
    public LambdaCounts getLambda(String model,
                                  NGram ngram) {
        cacheBuilder.withLambdas(model, Arrays.asList(ngram.getPattern()));

        LambdaCounts result = new LambdaCounts();
        for (int i = 0; i != ngram.size(); ++i)
            result.append(random.nextDouble());
        return result;
    }
}
