package de.glmtk.querying.estimator;

import de.glmtk.querying.estimator.backoff.BackoffEstimator;
import de.glmtk.querying.estimator.combination.CombinationEstimator;
import de.glmtk.querying.estimator.discount.AbsoluteDiscountEstimator;
import de.glmtk.querying.estimator.discount.ModifiedKneserNeyDiscountEstimator;
import de.glmtk.querying.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.FalseMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.MaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.interpolation.DiffInterpolationEstimator;
import de.glmtk.querying.estimator.interpolation.InterpolationEstimator;
import de.glmtk.querying.estimator.substitute.AbsoluteUnigramEstimator;
import de.glmtk.querying.estimator.substitute.ContinuationUnigramEstimator;
import de.glmtk.querying.estimator.substitute.UniformEstimator;

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

    // Backoff Estimators

    public static final BackoffEstimator BACKOFF_CMLE = new BackoffEstimator(
            CMLE, CMLE);

    public static final BackoffEstimator BACKOFF_CMLE_REC =
            new BackoffEstimator(CMLE);

    // Interpolation Estimators

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, MLE);

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_REC =
            new InterpolationEstimator(ABS_DISCOUNT_MLE);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_REC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE);

    // Combination Estimators

    public static final CombinationEstimator COMB_MLE_CMLE =
            new CombinationEstimator(MLE, CMLE, 0.75);

    // Combined Estimators

    public static final InterpolationEstimator MODIFIED_KNESER_NEY_ESIMATOR =
            new InterpolationEstimator(
                    new ModifiedKneserNeyDiscountEstimator(
                            new MaximumLikelihoodEstimator()),
                            new InterpolationEstimator(
                                    new ModifiedKneserNeyDiscountEstimator(
                                            new ContinuationMaximumLikelihoodEstimator())));

}
