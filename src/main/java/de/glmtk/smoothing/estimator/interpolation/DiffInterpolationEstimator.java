package de.glmtk.smoothing.estimator.interpolation;

import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.discount.DiscountEstimator;
import de.glmtk.utils.NGram;

public class DiffInterpolationEstimator extends InterpolationEstimator {

    public DiffInterpolationEstimator(
            DiscountEstimator alpha,
            Estimator beta) {
        super(alpha, beta);
    }

    public DiffInterpolationEstimator(
            DiscountEstimator alpha) {
        super(alpha);
    }

    @Override
    protected double
        calcProbability(NGram sequence, NGram history, int recDepth) {
        if (history.isEmptyOrOnlySkips()) {
            //if (history.isEmpty()) {
            return super.calcProbability(sequence, history, recDepth);
        } else {
            double alphaVal = alpha.probability(sequence, history, recDepth);
            double betaVal = 0;
            int cnt = 0;
            //            System.out.println(StringUtils.repeat("  ", recDepth) + history);
            for (int i = 0; i != history.size(); ++i) {
                NGram deriveHistory = history.differentiate(i, probMode);
                //                System.out.print(StringUtils.repeat("  ", recDepth) + "i=" + i
                //                        + " " + deriveHistory);
                if (history.equals(deriveHistory)) {
                    //                    System.out.println(" (x)");
                    continue;
                }
                if (deriveHistory.isEmptyOrOnlySkips()) {
                    //                    System.out.print(" (y)");
                }
                //System.out.println();
                ++cnt;
                betaVal += beta.probability(sequence, deriveHistory, recDepth);
            }
            betaVal /= cnt;
            //            if (cnt < history.size() - 1) {
            //                System.out.println(StringUtils.repeat("  ", recDepth)
            //                        + "LOWER  cnt=" + cnt + " < #history-1="
            //                        + (history.size() - 1));
            //            } else if (cnt > history.size() - 1) {
            //                System.out.println(StringUtils.repeat("  ", recDepth)
            //                        + "HIGHER cnt=" + cnt + " > #history-1="
            //                        + (history.size() - 1));
            //            }
            double gammaVal = gamma(sequence, history, recDepth);

            return alphaVal + gammaVal * betaVal;
        }
    }

}
