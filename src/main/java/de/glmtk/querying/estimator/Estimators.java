/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen, Rene Pickhardt
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.querying.estimator;

import de.glmtk.common.BackoffMode;
import de.glmtk.querying.estimator.backoff.BackoffEstimator;
import de.glmtk.querying.estimator.combination.CombinationEstimator;
import de.glmtk.querying.estimator.discount.AbsoluteDiscountEstimator;
import de.glmtk.querying.estimator.discount.ModKneserNeyDiscountEstimator;
import de.glmtk.querying.estimator.fast.FastGenLangModelAbsEstimator;
import de.glmtk.querying.estimator.fast.FastGenLangModelEstimator;
import de.glmtk.querying.estimator.fast.FastMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fast.FastModKneserNeyAbsEstimator;
import de.glmtk.querying.estimator.fast.FastModKneserNeyEstimator;
import de.glmtk.querying.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.FalseMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.MaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.interpol.DiffInterpolEstimator;
import de.glmtk.querying.estimator.interpol.InterpolEstimator;
import de.glmtk.querying.estimator.substitute.AbsoluteUnigramEstimator;
import de.glmtk.querying.estimator.substitute.ContinuationUnigramEstimator;
import de.glmtk.querying.estimator.substitute.UniformEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumAverageEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumGenLangModelEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumModKneserNeyEstimator;

public class Estimators {
    // Substitute Estimators ///////////////////////////////////////////////////

    public static final UniformEstimator UNIFORM = new UniformEstimator();
    static {
        UNIFORM.setName("Uniform");
    }

    public static final AbsoluteUnigramEstimator ABS_UNIGRAM = new AbsoluteUnigramEstimator();
    static {
        ABS_UNIGRAM.setName("Absolute-Unigram");
    }

    public static final ContinuationUnigramEstimator CONT_UNIGRAM = new ContinuationUnigramEstimator();
    static {
        CONT_UNIGRAM.setName("Continuation-Unigram");
    }

    // Fraction Estimators /////////////////////////////////////////////////////

    public static final MaximumLikelihoodEstimator MLE = new MaximumLikelihoodEstimator();
    static {
        MLE.setName("MaximumLikelihood");
    }

    public static final FalseMaximumLikelihoodEstimator FALSE_MLE = new FalseMaximumLikelihoodEstimator();
    static {
        FALSE_MLE.setName("FalseMaximumLikelihood");
    }

    public static final ContinuationMaximumLikelihoodEstimator CMLE = new ContinuationMaximumLikelihoodEstimator();
    static {
        CMLE.setName("ContinuationMaximumLikelihood");
    }

    // Discount Estimators /////////////////////////////////////////////////////

    public static final AbsoluteDiscountEstimator ABS_DISCOUNT_MLE = new AbsoluteDiscountEstimator(
            MLE, 0.75);
    static {
        ABS_DISCOUNT_MLE.setName("Absolute-Discount-MaximumLikelihood");
    }

    // Backoff Estimators //////////////////////////////////////////////////////

    public static final BackoffEstimator BACKOFF_CMLE_NOREC = new BackoffEstimator(
            CMLE, CMLE);
    static {
        BACKOFF_CMLE_NOREC.setName("Backoff-ContinuationMaximumLikelihood (Non-Recursive)");
    }

    public static final BackoffEstimator BACKOFF_CMLE = new BackoffEstimator(
            CMLE);
    static {
        BACKOFF_CMLE.setName("Backoff-Continuation MaximumlikeLikeLihood");
    }

    // Interpol Estimators /////////////////////////////////////////////////////

    public static final InterpolEstimator INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC = new InterpolEstimator(
            ABS_DISCOUNT_MLE, MLE, BackoffMode.SKP);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC.setName("Interpol-MaximumLikelihood (SKP Backoff, Non-Recursive)");
    }

    public static final InterpolEstimator INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC = new InterpolEstimator(
            ABS_DISCOUNT_MLE, MLE, BackoffMode.DEL);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC.setName("Interpol-MaximumLikelihood (DEL Backoff, Non-Recursive)");
    }

    public static final InterpolEstimator INTERPOL_ABS_DISCOUNT_MLE_SKP = new InterpolEstimator(
            ABS_DISCOUNT_MLE, BackoffMode.SKP);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_SKP.setName("Interpol-MaximumLikelihood (SKP Backoff)");
    }

    public static final InterpolEstimator INTERPOL_ABS_DISCOUNT_MLE_DEL = new InterpolEstimator(
            ABS_DISCOUNT_MLE, BackoffMode.DEL);
    static {
        INTERPOL_ABS_DISCOUNT_MLE_DEL.setName("Interpol-MaximumLikelihood (DEL Backoff)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, MLE, BackoffMode.SKP);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_NOREC.setName("DiffInterpol-MaximumLikelihood (SKP Backoff, Non-Recursive)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, MLE, BackoffMode.DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_NOREC.setName("DiffInterpol-MaximumLikelihood (DEL Backoff, Non-Recursive)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_NOREC = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, MLE, BackoffMode.DEL_FRONT);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT_NOREC.setName("DiffInterpol-MaximumLikelihood (DEL_FRONT Backoff, Non-Recursive)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_NOREC = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, MLE, BackoffMode.SKP_AND_DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL_NOREC.setName("DiffInterpol-MaximumLikelihood (SKP_AND_DEL Backoff, Non-Recursive)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, BackoffMode.SKP);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP.setName("DiffInterpol-MaximumLikelihood (SKP Backoff)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, BackoffMode.DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL.setName("DiffInterpol-MaximumLikelihood (DEL Backoff)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, BackoffMode.DEL_FRONT);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_DEL_FRONT.setName("DiffInterpol-MaximumLikelihood (DEL_FRONT Backoff)");
    }

    public static final DiffInterpolEstimator DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL = new DiffInterpolEstimator(
            ABS_DISCOUNT_MLE, BackoffMode.SKP_AND_DEL);
    static {
        DIFF_INTERPOL_ABS_DISCOUNT_MLE_SKP_AND_DEL.setName("DiffInterpol-MaximumLikelihood (SKP_AND_DEL Backoff)");
    }

    // Combination Estimators //////////////////////////////////////////////////

    public static final CombinationEstimator COMB_MLE_CMLE = new CombinationEstimator(
            MLE, CMLE, 0.75);
    static {
        COMB_MLE_CMLE.setName("Combination-MaximumLikeliehood-ContinuationMaximumLikelihood");
    }

    // Combined Estimators /////////////////////////////////////////////////////

    public static final InterpolEstimator MKN = makeMkn(BackoffMode.DEL);
    static {
        MKN.setName("Modified-Kneser-Ney");
    }

    public static final InterpolEstimator MKN_SKP = makeMkn(BackoffMode.SKP);
    static {
        MKN_SKP.setName("Modified-Kneser-Ney (SKP-Backoff)");
    }

    public static final InterpolEstimator MKN_ABS = new InterpolEstimator(
            new ModKneserNeyDiscountEstimator(new MaximumLikelihoodEstimator()),
            BackoffMode.DEL);
    static {
        MKN_ABS.setName("Modified-Kneser-Ney (Abs-Lower-Order)");
    }

    public static final DiffInterpolEstimator GLM = makeGlm(BackoffMode.SKP);
    static {
        GLM.setName("Generalized-Language-Model");
    }

    public static final DiffInterpolEstimator GLM_DEL = makeGlm(BackoffMode.DEL);
    static {
        GLM_DEL.setName("Generalized-Language-Model (DEL-Backoff)");
    }

    public static final DiffInterpolEstimator GLM_DEL_FRONT = makeGlm(BackoffMode.DEL_FRONT);
    static {
        GLM_DEL_FRONT.setName("Generalized-Language-Model (DEL-FRONT-Backoff)");
    }

    public static final DiffInterpolEstimator GLM_SKP_AND_DEL = makeGlm(BackoffMode.SKP_AND_DEL);
    static {
        GLM_SKP_AND_DEL.setName("Generalized-Language-Model (SKP-AND-DEL-Backoff)");
    }

    public static final DiffInterpolEstimator GLM_ABS = new DiffInterpolEstimator(
            new ModKneserNeyDiscountEstimator(new MaximumLikelihoodEstimator()),
            BackoffMode.SKP);
    static {
        GLM_ABS.setName("Generalized-Language-Model (Abs-Lower-Order)");
    }

    // Fast ////////////////////////////////////////////////////////////////////

    public static final FastMaximumLikelihoodEstimator FAST_MLE = new FastMaximumLikelihoodEstimator();
    static {
        FAST_MLE.setName("Fast-Maximum-Likelihood");
    }

    public static final FastModKneserNeyEstimator FAST_MKN = new FastModKneserNeyEstimator();
    static {
        FAST_MKN.setName("Fast-Modified-Kneser-Ney");
    }

    public static final FastModKneserNeyEstimator FAST_MKN_SKP = new FastModKneserNeyEstimator();
    static {
        FAST_MKN_SKP.setBackoffMode(BackoffMode.SKP);
        FAST_MKN_SKP.setName("Fast-Modified-Kneser-Ney (SKP Backoff)");
    }

    public static final FastModKneserNeyAbsEstimator FAST_MKN_ABS = new FastModKneserNeyAbsEstimator();
    static {
        FAST_MKN_ABS.setName("Fast-Modified-Kneser-Ney (Abs-Lower-Order)");
    }

    public static final FastGenLangModelEstimator FAST_GLM = new FastGenLangModelEstimator();
    static {
        FAST_GLM.setName("Fast-Generalized-Language-Model");
    }

    public static final FastGenLangModelEstimator FAST_GLM_DEL = new FastGenLangModelEstimator();
    static {
        FAST_GLM_DEL.setBackoffMode(BackoffMode.DEL);
        FAST_GLM_DEL.setName("Fast-Generalized-Language-Model (DEL-Backoff)");
    }

    public static final FastGenLangModelEstimator FAST_GLM_DEL_FRONT = new FastGenLangModelEstimator();
    static {
        FAST_GLM_DEL_FRONT.setBackoffMode(BackoffMode.DEL_FRONT);
        FAST_GLM_DEL_FRONT.setName("Fast-Generalized-Language-Model (DEL-FRONT-Backoff)");
    }

    public static final FastGenLangModelEstimator FAST_GLM_SKP_AND_DEL = new FastGenLangModelEstimator();
    static {
        FAST_GLM_SKP_AND_DEL.setBackoffMode(BackoffMode.SKP_AND_DEL);
        FAST_GLM_SKP_AND_DEL.setName("Fast-Generalized-Language-Model (SKP-AND-DEL-Backoff)");
    }

    public static final FastGenLangModelAbsEstimator FAST_GLM_ABS = new FastGenLangModelAbsEstimator();
    static {
        FAST_GLM_ABS.setName("Fast-Generalized-Language-Model (Abs-Lower-Order)");
    }

    // WeightedSum /////////////////////////////////////////////////////////////

    public static final WeightedSumAverageEstimator WEIGHTEDSUM_AVERAGE = new WeightedSumAverageEstimator();
    static {
        WEIGHTEDSUM_AVERAGE.setName("Weighted-Sum-Average");
    }

    public static final WeightedSumModKneserNeyEstimator WEIGHTEDSUM_MKN = new WeightedSumModKneserNeyEstimator();
    static {
        WEIGHTEDSUM_MKN.setName("Weighted-Sum-Modified-Kneser-Ney");
    }

    public static final WeightedSumModKneserNeyEstimator WEIGHTEDSUM_MKN_SKP = new WeightedSumModKneserNeyEstimator();
    static {
        WEIGHTEDSUM_MKN_SKP.setBackoffMode(BackoffMode.SKP);
        WEIGHTEDSUM_MKN_SKP.setName("Weighted-Sum-Modified-Kneser-Ney (SKP Backoff)");
    }

    public static final WeightedSumGenLangModelEstimator WEIGHTEDSUM_GLM = new WeightedSumGenLangModelEstimator();
    static {
        WEIGHTEDSUM_GLM.setName("Weighted-Sum-Generalized-Language-Model");
    }

    private static InterpolEstimator makeMkn(BackoffMode BackoffMode) {
        ModKneserNeyDiscountEstimator alpha = new ModKneserNeyDiscountEstimator(
                new MaximumLikelihoodEstimator());
        InterpolEstimator beta = new InterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new ContinuationMaximumLikelihoodEstimator()),
                        BackoffMode);
        return new InterpolEstimator(alpha, beta, BackoffMode);
    }

    private static DiffInterpolEstimator makeGlm(BackoffMode BackoffMode) {
        ModKneserNeyDiscountEstimator alpha = new ModKneserNeyDiscountEstimator(
                new MaximumLikelihoodEstimator());
        DiffInterpolEstimator beta = new DiffInterpolEstimator(
                new ModKneserNeyDiscountEstimator(
                        new ContinuationMaximumLikelihoodEstimator()),
                BackoffMode);
        return new DiffInterpolEstimator(alpha, beta, BackoffMode);
    }
}
