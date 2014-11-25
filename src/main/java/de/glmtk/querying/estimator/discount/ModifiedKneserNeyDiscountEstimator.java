package de.glmtk.querying.estimator.discount;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.glmtk.Constants;
import de.glmtk.querying.estimator.fraction.FractionEstimator;
import de.glmtk.utils.CountCache;
import de.glmtk.utils.NGram;
import de.glmtk.utils.Pattern;
import de.glmtk.utils.PatternCalculator;
import de.glmtk.utils.PatternElem;

public class ModifiedKneserNeyDiscountEstimator extends DiscountEstimator {

    private Map<Pattern, Double> discount1 = null;

    private Map<Pattern, Double> discount2 = null;

    private Map<Pattern, Double> discount3p = null;

    public ModifiedKneserNeyDiscountEstimator(
            FractionEstimator fractionEstimator) {
        super(fractionEstimator);
    }

    @Override
    public void setCountCache(CountCache countCache) {
        super.setCountCache(countCache);

        // TODO calc discount
        discount1 = new HashMap<Pattern, Double>();
        discount2 = new HashMap<Pattern, Double>();
        discount3p = new HashMap<Pattern, Double>();
        for (Pattern pattern : PatternCalculator.getCombinations(Constants.MODEL_SIZE,
                Arrays.asList(PatternElem.CNT, PatternElem.SKP))) {
            long[] n = countCache.getNGramTimes(pattern);
            double y = (double) n[0] / (n[0] + n[1]);
            discount1.put(pattern, 1.f - 2.f * y * n[1] / n[0]);
            discount2.put(pattern, 2.f - 3.f * y * n[2] / n[1]);
            discount3p.put(pattern, 3.f - 4.f * y * n[3] / n[2]);
        }
    }

    @Override
    protected double calcDiscount(NGram sequence, NGram history, int recDepth) {
        switch ((int) countCache.getAbsolute(history)) {
            case 0:
                return 0;
            case 1:
                return discount1.get(history.getPattern());
            case 2:
                return discount2.get(history.getPattern());
            default:
                return discount3p.get(history.getPattern());
        }
    }

    public double getDiscount1(Pattern pattern) {
        return discount1.get(pattern);
    }

    public double getDiscount2(Pattern pattern) {
        return discount2.get(pattern);
    }

    public double getDiscount3p(Pattern pattern) {
        return discount3p.get(pattern);
    }

}
