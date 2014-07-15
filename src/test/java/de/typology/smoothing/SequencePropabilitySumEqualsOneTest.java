package de.typology.smoothing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

public class SequencePropabilitySumEqualsOneTest extends AbstractEstimatorTest {

    protected static Logger logger = LogManager
            .getLogger(SequencePropabilitySumEqualsOneTest.class);

    @Override
    protected void testEstimator(
            String testName,
            Estimator estimator,
            int maxOrder) {
        logger.info("===== {} =====", testName);
        testEstimatorCalculator(estimator, new SkipCalculator(estimator),
                maxOrder);
        testEstimatorCalculator(estimator, new DeleteCalculator(estimator),
                maxOrder);
        // add more calculator here
    }

    protected void testEstimatorCalculator(
            Estimator estimator,
            PropabilityCalculator calculator,
            int maxOrder) {
        logger.info("=== {}", calculator.getClass().getSimpleName());
        testEstimatorCalculatorCorpus(estimator, calculator, abcCorpus,
                abcTestCorpus, maxOrder);
        testEstimatorCalculatorCorpus(estimator, calculator, mobyDickCorpus,
                mobyDickTestCorpus, maxOrder);
        // add more corpora here
    }

    protected void testEstimatorCalculatorCorpus(
            Estimator estimator,
            PropabilityCalculator calculator,
            Corpus corpus,
            TestCorpus testCorpus,
            int maxOrder) {
        logger.info("# {} corpus", testCorpus.getWorkingDir().getFileName());

        estimator.setCorpus(corpus);
        for (int order = 1; order != maxOrder; ++order) {
            double sum = 0;
            for (int i = 0; i != (int) Math.pow(testCorpus.getWords().length,
                    order); ++i) {
                String sequence = testCorpus.getSequenceString(i, order);
                sum += calculator.propability(sequence);
            }
            logger.info("n={}: sum = {}", order, sum);
            Assert.assertEquals(1, sum, 0.01);
        }
        logger.info("passed");
    }

}
