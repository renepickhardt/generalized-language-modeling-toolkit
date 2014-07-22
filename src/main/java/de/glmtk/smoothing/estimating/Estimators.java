package de.glmtk.smoothing.estimating;

public class Estimators {

    public static final Estimator UNIFORM_ESTIMATOR = new UniformEstimator();

    public static final Estimator ABSOLUTE_UNIGRAM_ESTIMATOR =
            new AbsoluteUnigramEstimator();

    public static final Estimator CONTINUATION_UNIGRAM_ESTIMATOR =
            new ContinuationUnigramEstimator();

    public static final Estimator MLE = new MaximumLikelihoodEstimator();

    public static final Estimator CMLE =
            new ContinuationMaximumLikelihoodEstimator();

    public static final Estimator FMLE = new FalseMaximumLikelihoodEstimator();

}
