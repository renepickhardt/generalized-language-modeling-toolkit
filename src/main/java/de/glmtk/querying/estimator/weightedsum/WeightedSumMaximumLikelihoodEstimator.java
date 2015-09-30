package de.glmtk.querying.estimator.weightedsum;

import static de.glmtk.common.NGram.SKP_NGRAM;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;


public class WeightedSumMaximumLikelihoodEstimator extends
                                                   AbstractWeightedSumEstimator {
    @Override
    public WeightedSumFunction calcWeightedSumFunction(NGram history) {
        WeightedSumFunction weightedSumFunction = new WeightedSumFunction(1);

        NGram fullHistory = history.concat(SKP_NGRAM);
        while (!fullHistory.seen(cache)) {
            fullHistory = fullHistory.backoff(BackoffMode.DEL);
        }

        NGram hist = fullHistory.remove(fullHistory.size() - 1);
        double count = cache.getCount(fullHistory);
        weightedSumFunction.add(1.0 / count, hist);

        return weightedSumFunction;
    }

    @Override
    protected double calcAlpha(NGram sequence) {
        return cache.getCount(sequence);
    }
}
