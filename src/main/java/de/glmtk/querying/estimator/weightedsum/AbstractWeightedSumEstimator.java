package de.glmtk.querying.estimator.weightedsum;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.cache.Cache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Discounts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.querying.estimator.AbstractEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;

public abstract class AbstractWeightedSumEstimator extends AbstractEstimator implements WeightedSumEstimator {
    private Map<Pattern, Discounts> discounts = new HashMap<>();

    @Override
    public void setCache(Cache cache) {
        super.setCache(cache);
        discounts = new HashMap<>();
    }

    @Override
    public double probability(NGram sequence,
                              WeightedSumFunction weightedSumFunction) {
        return calcProbability(sequence, weightedSumFunction, 1);
    }

    @Override
    public double calcProbability(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        WeightedSumFunction weightedSumFunction = calcWeightedSumFunction(history);
        return calcProbability(sequence, weightedSumFunction, recDepth);
    }

    public double calcProbability(NGram sequence,
                                  WeightedSumFunction weightedSumFunction,
                                  @SuppressWarnings("unused") int recDepth) {
        double prob = 0.0;

        for (Summand summand : weightedSumFunction) {
            NGram fullSequence = getFullSequence(sequence, summand.getHistory());
            prob += summand.getWeight() * calcAlpha(fullSequence);
        }

        return prob;
    }

    protected double calcAlpha(NGram sequence) {
        long count = cache.getCount(sequence), absSequenceCount = count;

        if (!sequence.getPattern().isAbsolute()) {
            sequence = sequence.remove(0).convertWskpToSkp();
            absSequenceCount = cache.getCount(sequence);
        }

        if (sequence.getPattern().numElems(PatternElem.CNT) == 1)
            // If we are on last order don't discount.
            return count;

        Discounts disc = calcDiscounts(sequence.getPattern());
        double d = disc.getForCount(absSequenceCount);

        return Math.max(count - d, 0.0);
    }

    protected Discounts calcDiscounts(Pattern pattern) {
        Discounts result = discounts.get(pattern);
        if (result != null)
            return result;

        NGramTimes n = cache.getNGramTimes(pattern);
        double y = (double) n.getOneCount()
                / (n.getOneCount() + n.getTwoCount());
        result = new Discounts(1.0f - 2.0f * y * n.getTwoCount()
                / n.getOneCount(), 2.0f - 3.0f * y * n.getThreeCount()
                / n.getTwoCount(), 3.0f - 4.0f * y * n.getFourCount()
                / n.getThreeCount());

        discounts.put(pattern, result);
        return result;
    }
}
