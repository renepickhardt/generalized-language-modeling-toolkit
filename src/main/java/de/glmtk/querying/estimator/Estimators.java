package de.glmtk.querying.estimator;

import de.glmtk.common.BackoffMode;
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

    // Substitute ParamEstimators

    public static final UniformEstimator UNIFORM = new UniformEstimator();
    static {
        UNIFORM.setName("Uniform");
    }

    public static final AbsoluteUnigramEstimator ABS_UNIGRAM =
            new AbsoluteUnigramEstimator();
    static {
        ABS_UNIGRAM.setName("Absolute-Unigram");
    }

    public static final ContinuationUnigramEstimator CONT_UNIGRAM =
            new ContinuationUnigramEstimator();
    static {
        CONT_UNIGRAM.setName("Continuation-Unigram");
    }

    // Fraction ParamEstimators

    public static final MaximumLikelihoodEstimator MLE =
            new MaximumLikelihoodEstimator();
    static {
        MLE.setName("MaximumLikelihood");
    }

    public static final FalseMaximumLikelihoodEstimator FMLE =
            new FalseMaximumLikelihoodEstimator();
    static {
        FMLE.setName("FalseMaximumLikelihood");
    }

    public static final ContinuationMaximumLikelihoodEstimator CMLE =
            new ContinuationMaximumLikelihoodEstimator();
    static {
        CMLE.setName("ContinuationMaximumLikelihood");
    }

    // Discount ParamEstimators

    public static final AbsoluteDiscountEstimator ABS_DISCOUNT_MLE =
            new AbsoluteDiscountEstimator(MLE, 0.75);
    static {
        ABS_DISCOUNT_MLE.setName("Absolute-Discount-MaximumLikelihood");
    }

    // Backoff ParamEstimators

    public static final BackoffEstimator BACKOFF_CMLE_NOREC =
            new BackoffEstimator(CMLE, CMLE);
    static {
        BACKOFF_CMLE_NOREC
        .setName("Backoff-ContinuationMaximumLikelihood (Non-Recursive)");
    }

    public static final BackoffEstimator BACKOFF_CMLE = new BackoffEstimator(
            CMLE);
    static {
        BACKOFF_CMLE.setName("Backoff-Continuation MaximumlikeLikeLihood");
    }

    // Interpolation ParamEstimators

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, MLE, BackoffMode.SKP);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC
        .setName("Interpolation-MaximumLikelihood (SKP Backoff, Non-Recursive)");
    }

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, MLE, BackoffMode.DEL);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC
        .setName("Interpolation-MaximumLikelihood (DEL Backoff, Non-Recursive)");
    }

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_SKP =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.SKP);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_SKP
        .setName("Interpolation-MaximumLikelihood (SKP Backoff)");
    }

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE_DEL =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.DEL);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_DEL
        .setName("Interpolation-MaximumLikelihood (DEL Backoff)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.SKP);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC
        .setName("DiffInterpolation-MaximumLikelihood (SKP Backoff, Non-Recursive)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC
        .setName("DiffInterpolation-MaximumLikelihood (DEL Backoff, Non-Recursive)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_NOREC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.DEL_FRONT);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_NOREC
        .setName("DiffInterpolation-MaximumLikelihood (DEL_FRONT Backoff, Non-Recursive)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_NOREC =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, MLE,
                    BackoffMode.SKP_AND_DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_NOREC
        .setName("DiffInterpolation-MaximumLikelihood (SKP_AND_DEL Backoff, Non-Recursive)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.SKP);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP
        .setName("DiffInterpolation-MaximumLikelihood (SKP Backoff)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE, BackoffMode.DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL
        .setName("DiffInterpolation-MaximumLikelihood (DEL Backoff)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE,
                    BackoffMode.DEL_FRONT);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT
        .setName("DiffInterpolation-MaximumLikelihood (DEL_FRONT Backoff)");
    }

    public static final DiffInterpolationEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL =
            new DiffInterpolationEstimator(ABS_DISCOUNT_MLE,
                    BackoffMode.SKP_AND_DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL
        .setName("DiffInterpolation-MaximumLikelihood (SKP_AND_DEL Backoff)");
    }

    // Combination ParamEstimators

    public static final CombinationEstimator COMB_MLE_CMLE =
            new CombinationEstimator(MLE, CMLE, 0.75);
    static {
        COMB_MLE_CMLE
        .setName("Combination-MaximumLikeliehood-ContinuationMaximumLikelihood");
    }

    // Combined ParamEstimators

    private static InterpolationEstimator makeMkn(BackoffMode BackoffMode) {
        return new InterpolationEstimator(
                new ModifiedKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                        new InterpolationEstimator(
                                new ModifiedKneserNeyDiscountEstimator(
                                        new ContinuationMaximumLikelihoodEstimator()),
                                        BackoffMode), BackoffMode);
    }

    public static final InterpolationEstimator MOD_KNESER_NEY =
            makeMkn(BackoffMode.DEL);
    static {
        MOD_KNESER_NEY.setName("Modified-Kneser-Ney");
    }

    public static final InterpolationEstimator MOD_KNESER_NEY_SKP =
            makeMkn(BackoffMode.SKP);
    static {
        MOD_KNESER_NEY_SKP.setName("Modified-Kneser-Ney (SKP-Backoff)");
    }

    public static final InterpolationEstimator MOD_KNESER_NEY_ABS =
            new InterpolationEstimator(new ModifiedKneserNeyDiscountEstimator(
                    new MaximumLikelihoodEstimator()), BackoffMode.DEL);
    static {
        MOD_KNESER_NEY_ABS.setName("Modified-Kneser-Ney (Abs-Lower-Order)");
    }

    private static DiffInterpolationEstimator makeGlm(BackoffMode BackoffMode) {
        return new DiffInterpolationEstimator(
                new ModifiedKneserNeyDiscountEstimator(
                        new MaximumLikelihoodEstimator()),
                        new DiffInterpolationEstimator(
                                new ModifiedKneserNeyDiscountEstimator(
                                        new ContinuationMaximumLikelihoodEstimator()),
                                        BackoffMode), BackoffMode);
    }

    public static final DiffInterpolationEstimator GLM =
            makeGlm(BackoffMode.SKP);
    static {
        GLM.setName("Generalized-Language-Model");
    }

    public static final DiffInterpolationEstimator GLM_DEL =
            makeGlm(BackoffMode.DEL);
    static {
        GLM_DEL.setName("Generalized-Language-Model (DEL-Backoff)");
    }

    public static final DiffInterpolationEstimator GLM_DEL_FRONT =
            makeGlm(BackoffMode.DEL_FRONT);
    static {
        GLM_DEL_FRONT.setName("Generalized-Language-Model (DEL-FRONT-Backoff)");
    }

    public static final DiffInterpolationEstimator GLM_SKP_AND_DEL =
            makeGlm(BackoffMode.SKP_AND_DEL);
    static {
        GLM_SKP_AND_DEL
        .setName("Generalized-Language-Model (SKP-AND-DEL-Backoff)");
    }

    public static final DiffInterpolationEstimator GLM_ABS =
            new DiffInterpolationEstimator(
                    new ModifiedKneserNeyDiscountEstimator(
                            new MaximumLikelihoodEstimator()), BackoffMode.SKP);
    static {
        GLM_ABS.setName("Generalized-Language-Model (Abs-Lower-Order)");
    }

}
