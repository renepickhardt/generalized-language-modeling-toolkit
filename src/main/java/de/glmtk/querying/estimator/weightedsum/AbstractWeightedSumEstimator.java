package de.glmtk.querying.estimator.weightedsum;

import de.glmtk.common.NGram;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Discounts;
import de.glmtk.querying.estimator.AbstractEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;

public abstract class AbstractWeightedSumEstimator extends AbstractEstimator implements WeightedSumEstimator {
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

        Discounts discounts = cache.getDiscounts(sequence.getPattern());
        double d = discounts.getForCount(absSequenceCount);

        return Math.max(count - d, 0.0);
    }
}
