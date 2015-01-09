package de.glmtk.querying;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.common.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.calculator.SequenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastModifiedKneserNeyEstimator;
import de.glmtk.testutil.LoggingTest;
import de.glmtk.testutil.TestCorpus;

public class FastEstimatorTest extends LoggingTest {
    @Test
    public void testFastModifiedKneserEstimator() throws Exception {
        FastModifiedKneserNeyEstimator fastEstimator = new FastModifiedKneserNeyEstimator();
        Estimator estimator = Estimators.MOD_KNESER_NEY;
        testFastEstimator(fastEstimator, estimator);
    }

    private void testFastEstimator(Estimator fastEstimator,
                                   Estimator estimator) throws Exception {
        TestCorpus testCorpus = TestCorpus.EN0008T;

        Calculator calculator = new SequenceCalculator();
        calculator.setEstimator(estimator);
        calculator.setProbMode(ProbMode.MARG);
        estimator.setCountCache(testCorpus.getCountCache());

        List<String> sequence = Arrays.asList("4", ".", "3", "speak", "an");
        System.out.println(calculator.probability(sequence));
    }
}
