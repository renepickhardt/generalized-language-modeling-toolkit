package de.glmtk.querying.estimator.weightedsum;

import static de.glmtk.common.NGram.WSKP_NGRAM;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.cache.Cache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
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
            prob += summand.getWeight()
                    * calcAlpha(fullSequence, summand.isAbsolute(),
                            summand.isDiscounted());
        }

        return prob;
    }

    /**
     * @param absolute
     *            If {@code true} alpha absolute count, if {@code false} alpha
     *            continuation count.
     * @param discount
     *            If {@code true} discount alpha if {@code false} not.
     */
    protected double calcAlpha(NGram sequence,
                               boolean absolute,
                               boolean discount) {
        long absSequenceCount = cache.getAbsolute(sequence);
        double alpha;
        if (absolute)
            alpha = absSequenceCount;
        else
            alpha = cache.getContinuation(
                    WSKP_NGRAM.concat(sequence.convertSkpToWskp())).getOnePlusCount();

        if (discount) {
            Discounts disc = calcDiscounts(sequence.getPattern());
            double d = disc.getForCount(absSequenceCount);

            alpha = Math.max(alpha - d, 0.0);
        }

        return alpha;
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
