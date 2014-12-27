package de.glmtk.querying.estimator;

import java.lang.reflect.Field;

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
import de.glmtk.utils.BackoffMode;

public class Estimators {

    // Substitute ParamEstimators

    public static final UniformEstimator UNIFORM = new UniformEstimator();

    public static final AbsoluteUnigramEstimator ABS_UNIGRAM =
            new AbsoluteUnigramEstimator();

    public static final ContinuationUnigramEstimator CONT_UNIGRAM =
            new ContinuationUnigramEstimator();

    // Fraction ParamEstimators

    public static final MaximumLikelihoodEstimator MLE =
            new MaximumLikelihoodEstimator();

    public static final FalseMaximumLikelihoodEstimator FMLE =
            new FalseMaximumLikelihoodEstimator();

    public static final ContinuationMaximumLikelihoodEstimator CMLE =
            new ContinuationMaximumLikelihoodEstimator();

    // Discount ParamEstimators

    public static final AbsoluteDiscountEstimator ABS_DISCOUNT_MLE =
            new AbsoluteDiscountEstimator(MLE, 0.75);

    // Backoff ParamEstimators

    public static final BackoffEstimator BACKOFF_CMLE = new BackoffEstimator(
            CMLE, CMLE);

    public static final BackoffEstimator BACKOFF_CMLE_REC =
            new BackoffEstimator(CMLE);

    // Interpolation ParamEstimators

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_SKP =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, MLE, BackoffMode.SKP);

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_DEL =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, MLE, BackoffMode.DEL);

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_SKP_REC =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.SKP);

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_DEL_REC =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.DEL);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.SKP);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.DEL);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.DEL_FRONT);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.SKP_AND_DEL);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_REC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.SKP);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_REC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.DEL);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_REC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE,
                    BackoffMode.DEL_FRONT);

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_REC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE,
                    BackoffMode.SKP_AND_DEL);

    // Combination ParamEstimators

    public static final CombinationEstimator COMB_MLE_CMLE =
            new CombinationEstimator(MLE, CMLE, 0.75);

    // Combined ParamEstimators

    private static InterpolationEstimator makeMkn(BackoffMode backoffMode) {
        return new InterpolationEstimator(
                new ModifiedKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                        new InterpolationEstimator(
                                new ModifiedKneserNeyDiscountEstimator(
                                        new ContinuationMaximumLikelihoodEstimator()),
                                        backoffMode), backoffMode);
    }

    public static final InterpolationEstimator MOD_KNESER_NEY =
            makeMkn(BackoffMode.DEL);

    public static final InterpolationEstimator MOD_KNESER_NEY_SKP =
            makeMkn(BackoffMode.SKP);

    public static final InterpolationEstimator MOD_KNESER_NEY_ABS =
            new InterpolationEstimator(new ModifiedKneserNeyDiscountEstimator(
                    new MaximumLikelihoodEstimator()), BackoffMode.DEL);

    private static DiffInterpolationEstimator makeGlm(BackoffMode backoffMode) {
        return new DiffInterpolationEstimator(
                new ModifiedKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                        new DiffInterpolationEstimator(
                                new ModifiedKneserNeyDiscountEstimator(
                                        new ContinuationMaximumLikelihoodEstimator()),
                                        backoffMode), backoffMode);
    }

    public static final DiffInterpolationEstimator GLM =
            makeGlm(BackoffMode.SKP);

    public static final DiffInterpolationEstimator GLM_DEL =
            makeGlm(BackoffMode.DEL);

    public static final DiffInterpolationEstimator GLM_DEL_FRONT =
            makeGlm(BackoffMode.DEL_FRONT);

    public static final DiffInterpolationEstimator GLM_SKP_AND_DEL =
            makeGlm(BackoffMode.SKP_AND_DEL);

    public static final DiffInterpolationEstimator GLM_ABS =
            new DiffInterpolationEstimator(
                    new ModifiedKneserNeyDiscountEstimator(
                            new MaximumLikelihoodEstimator()), BackoffMode.SKP);

    public static String getName(Estimator estimator) {
        try {
            for (Field field : Estimators.class.getDeclaredFields()) {
                if (estimator.equals(field.get(null))) {
                    return field.getName();
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
        }
        return "";
    }

}
