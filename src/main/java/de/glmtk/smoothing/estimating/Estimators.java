package de.glmtk.smoothing.estimating;

public class Estimators {

    public static final UniformEstimator UNIFORM = new UniformEstimator();

    public static final AbsoluteUnigramEstimator ABSOLUTE_UNIGRAM =
            new AbsoluteUnigramEstimator();

    public static final ContinuationUnigramEstimator CONTINUATION_UNIGRAM =
            new ContinuationUnigramEstimator();

    public static final MaximumLikelihoodEstimator MLE =
            new MaximumLikelihoodEstimator();

    public static final ContinuationMaximumLikelihoodEstimator CMLE =
            new ContinuationMaximumLikelihoodEstimator();

    public static final FalseMaximumLikelihoodEstimator FMLE =
            new FalseMaximumLikelihoodEstimator();

    public static final AbsoluteDiscountEstimator ABS_DISCOUNT_MLE =
            new AbsoluteDiscountEstimator(new MaximumLikelihoodEstimator(),
                    0.75);

    public static final InterpolationEstimator INTERPOL_ABS_DISCOUNT_MLE =
            new InterpolationEstimator(ABS_DISCOUNT_MLE, MLE);

}
