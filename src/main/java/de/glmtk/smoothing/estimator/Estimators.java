package de.glmtk.smoothing.estimator;

import de.glmtk.smoothing.estimator.combination.CombinationEstimator;
import de.glmtk.smoothing.estimator.discount.AbsoluteDiscountEstimator;
import de.glmtk.smoothing.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.smoothing.estimator.fraction.FalseMaximumLikelihoodEstimator;
import de.glmtk.smoothing.estimator.fraction.MaximumLikelihoodEstimator;
import de.glmtk.smoothing.estimator.interpolation.InterpolationEstimator;
import de.glmtk.smoothing.estimator.substitute.AbsoluteUnigramEstimator;
import de.glmtk.smoothing.estimator.substitute.ContinuationUnigramEstimator;
import de.glmtk.smoothing.estimator.substitute.UniformEstimator;

public class Estimators {

    // Substitute Estimators

    public static final UniformEstimator UNIFORM = new UniformEstimator();

    public static final AbsoluteUnigramEstimator ABS_UNIGRAM =
            new AbsoluteUnigramEstimator();

    public static final ContinuationUnigramEstimator CONT_UNIGRAM =
            new ContinuationUnigramEstimator();

    // Fraction Estimators

    public static final MaximumLikelihoodEstimator MLE =
            new MaximumLikelihoodEstimator();

    public static final FalseMaximumLikelihoodEstimator FMLE =
            new FalseMaximumLikelihoodEstimator();

    public static final ContinuationMaximumLikelihoodEstimator CMLE =
            new ContinuationMaximumLikelihoodEstimator();

    // Discount Estimators

    public static final AbsoluteDiscountEstimator ABS_DISCOUNT_MLE =
            new AbsoluteDiscountEstimator(MLE, 0.75);

    // Interpolation Estimators

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, MLE);

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_REC =
            new InterpolationEstimator(ABS_DISCOUNT_MLE);

    // Combination Estimators

    public static final CombinationEstimator COMB_MLE_CMLE =
            new CombinationEstimator(MLE, CMLE, 0.75);

}
