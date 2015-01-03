package de.glmtk.querying.estimator.discount;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.common.CountCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.querying.estimator.fraction.FractionEstimator;

public class ModifiedKneserNeyDiscountEstimator extends DiscountEstimator {
    private Map<Pattern, double[]> discounts = null;

    public ModifiedKneserNeyDiscountEstimator(FractionEstimator fractionEstimator) {
        super(fractionEstimator);
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);

        discounts = new HashMap<Pattern, double[]>();
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

        long[] n = countCache.getNGramTimes(pattern);
        double y = (double) n[0] / (n[0] + n[1]);
        result = new double[] {1.0f - 2.0f * y * n[1] / n[0],
                2.0f - 3.0f * y * n[2] / n[1], 3.0f - 4.0f * y * n[3] / n[2]};
        discounts.put(pattern, result);
        return result;
    }
}
