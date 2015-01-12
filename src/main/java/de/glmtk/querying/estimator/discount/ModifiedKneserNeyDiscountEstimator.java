package de.glmtk.querying.estimator.discount;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.counts.NGramTimes;
import de.glmtk.querying.estimator.fraction.FractionEstimator;

public class ModifiedKneserNeyDiscountEstimator extends DiscountEstimator {
    private Map<Pattern, double[]> discounts = null;

    public ModifiedKneserNeyDiscountEstimator(FractionEstimator fractionEstimator) {
        super(fractionEstimator);
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);

        discounts = new HashMap<>();
    }

    @Override
    protected double calcDiscount(NGram sequence,
                                  NGram history,
                                  int recDepth) {
        double[] discounts = getDiscounts(history.getPattern());
        switch ((int) countCache.getAbsolute(history)) {
            case 0:
                return 0;
            case 1:
                return discounts[0];
            case 2:
                return discounts[1];
            default:
                return discounts[2];
        }
    }

    public double[] getDiscounts(Pattern pattern) {
        double[] result = discounts.get(pattern);
        if (result != null)
            return result;

        NGramTimes n = countCache.getNGramTimes(pattern);
        double y = (double) n.getOneCount()
                / (n.getOneCount() + n.getTwoCount());
        result = new double[] {
                1.0f - 2.0f * y * n.getTwoCount() / n.getOneCount(),
                2.0f - 3.0f * y * n.getThreeCount() / n.getTwoCount(),
                3.0f - 4.0f * y * n.getFourCount() / n.getThreeCount()};
        discounts.put(pattern, result);
        return result;
    }
}
