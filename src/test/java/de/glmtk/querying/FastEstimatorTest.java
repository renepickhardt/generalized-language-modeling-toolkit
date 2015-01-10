package de.glmtk.querying;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import de.glmtk.common.CountCache;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.common.ProbMode;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.calculator.SequenceCalculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;
import de.glmtk.querying.estimator.fast.FastModifiedKneserNeyAbsEstimator;
import de.glmtk.testutil.TestCorporaTest;
import de.glmtk.testutil.TestCorpus;

public class FastEstimatorTest extends TestCorporaTest {
    private static final Logger LOGGER = LogManager.getFormatterLogger(FastEstimatorTest.class);

    @Test
    public void testFastModifiedKneserEstimator() throws Exception {
        Estimator estimator = Estimators.MOD_KNESER_NEY_ABS;
        FastModifiedKneserNeyAbsEstimator fastEstimator = new FastModifiedKneserNeyAbsEstimator();
        testEstimatorEquals(estimator, fastEstimator);
    }

    private void testEstimatorEquals(Estimator estimatorExpected,
                                     Estimator estimatorActual) throws Exception {
        Set<Pattern> patternsExpected = Patterns.getUsedPatterns(5,
                estimatorExpected, ProbMode.MARG);
        Set<Pattern> patternsActual = Patterns.getUsedPatterns(5,
                estimatorActual, ProbMode.MARG);
        Set<Pattern> patterns = new HashSet<>();
        patterns.addAll(patternsExpected);
        patterns.addAll(patternsActual);

        LOGGER.debug("patternsExpcted = %s", patternsExpected);
        LOGGER.debug("patternsActual  = %s", patternsActual);
        LOGGER.debug("patterns        = %s", patterns);

        TestCorpus testCorpus = TestCorpus.EN0008T;
        CountCache countCache = testCorpus.getCountCache(patterns);

        Calculator calculatorExpected = new SequenceCalculator();
        calculatorExpected.setEstimator(estimatorExpected);
        calculatorExpected.setProbMode(ProbMode.MARG);
        estimatorExpected.setCountCache(countCache);

        Calculator calculatorActual = new SequenceCalculator();
        calculatorActual.setEstimator(estimatorActual);
        calculatorActual.setProbMode(ProbMode.MARG);
        estimatorActual.setCountCache(countCache);

        List<String> sequence = Arrays.asList("4", ".", "3", "speak", "an");
        System.out.println(calculatorExpected.probability(sequence));
        System.out.println(calculatorActual.probability(sequence));
    }
}
