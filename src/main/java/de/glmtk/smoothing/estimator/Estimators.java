package de.glmtk.smoothing.estimator;

import de.glmtk.smoothing.estimator.fraction.ContinuationMaximumLikelihoodEstimator;
import de.glmtk.smoothing.estimator.fraction.FalseMaximumLikelihoodEstimator;
import de.glmtk.smoothing.estimator.fraction.MaximumLikelihoodEstimator;
import de.glmtk.smoothing.estimator.substitute.AbsoluteUnigramEstimator;
import de.glmtk.smoothing.estimator.substitute.ContinuationUnigramEstimator;
import de.glmtk.smoothing.estimator.substitute.UniformEstimator;

public class Estimators {

    // Substitute

    public static final UniformEstimator UNIFORM = new UniformEstimator();

    public static final AbsoluteUnigramEstimator ABS_UNIGRAM =
            new AbsoluteUnigramEstimator();

    public static final ContinuationUnigramEstimator CONT_UNIGRAM =
            new ContinuationUnigramEstimator();

    // Fraction

    public static final MaximumLikelihoodEstimator MLE =
            new MaximumLikelihoodEstimator();

    public static final FalseMaximumLikelihoodEstimator FMLE =
            new FalseMaximumLikelihoodEstimator();

    public static final ContinuationMaximumLikelihoodEstimator CMLE =
            new ContinuationMaximumLikelihoodEstimator();

}
