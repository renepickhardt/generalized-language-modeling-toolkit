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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.glmtk.common.BackoffMode;
import de.glmtk.querying.estimator.backoff.BackoffEstimator;
import de.glmtk.querying.estimator.combination.CombinationEstimator;
import de.glmtk.querying.estimator.discount.AbsoluteDiscountEstimator;
import de.glmtk.querying.estimator.discount.ModKneserNeyDiscountEstimator;
import de.glmtk.querying.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.FalseMaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.fraction.MaximumLikelihoodEstimator;
import de.glmtk.querying.estimator.interpol.DiffInterpolEstimator;
import de.glmtk.querying.estimator.interpol.InterpolEstimator;
import de.glmtk.querying.estimator.substitute.AbsoluteUnigramEstimator;
import de.glmtk.querying.estimator.substitute.ContinuationUnigramEstimator;
import de.glmtk.querying.estimator.substitute.UniformEstimator;

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

    public static final FalseMaximumLikelihoodEstimator FMLE = new FalseMaximumLikelihoodEstimator();
    static {
        FMLE.setName("FalseMaximumLikelihood");
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

    public static final InterpolEstimator MOD_KNESER_NEY = makeMkn(BackoffMode.DEL);
    static {
        MOD_KNESER_NEY.setName("Modified-Kneser-Ney");
    }

    public static final InterpolEstimator MOD_KNESER_NEY_SKP = makeMkn(BackoffMode.SKP);
    static {
        MOD_KNESER_NEY_SKP.setName("Modified-Kneser-Ney (SKP-Backoff)");
    }

    public static final InterpolEstimator MOD_KNESER_NEY_ABS = new InterpolEstimator(
            new ModKneserNeyDiscountEstimator(new MaximumLikelihoodEstimator()),
            BackoffMode.DEL);
    static {
        MOD_KNESER_NEY_ABS.setName("Modified-Kneser-Ney (Abs-Lower-Order)");
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

    // Collections /////////////////////////////////////////////////////////////

    public static final Set<Estimator> MOD_KNESER_NEY_ESTIMATORS = new HashSet<Estimator>(
            Arrays.asList(MOD_KNESER_NEY, MOD_KNESER_NEY_ABS,
                    MOD_KNESER_NEY_SKP));

    public static final Set<Estimator> GLM_ESTIMATORS = new HashSet<Estimator>(
            Arrays.asList(GLM, GLM_ABS, GLM_DEL, GLM_DEL, GLM_SKP_AND_DEL));

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
