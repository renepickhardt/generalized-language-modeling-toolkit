package de.typology.smoothing;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class SmootherSumTest extends AbcCorpusTest {

    @Test
    public void testMaximumLikelihoodSmoother() throws IOException {
        Smoother smoother = newMaximumLikelihoodSmoother();
        testSum(smoother, 5);
    }

    @Test
    public void testDiscountSmoother() throws IOException {
        Smoother smoother = newDiscountSmoother(1.);
        testSum(smoother, 5);
    }

    @Test
    public void testPropabilityCond2Smoother() throws IOException {
        Smoother smoother = newPropabilityCond2Smoother(1.);
        testSum(smoother, 5);
    }

    @Test
    public void testInterpolatedKneserNeySmoother() throws IOException {
        Smoother smoother = newInterpolatedKneserNeySmoother();
        testSum(smoother, 5);
    }

    private void testSum(Smoother smoother, int length) {
        System.out.println(smoother.getClass().getSimpleName());
        for (int i = 1; i != length + 1; ++i) {
            double sum = 0;
            for (int j = 0; j != ((int) Math.pow(3, i)); ++j) {
                sum += smoother.propability(getAbcSequence(j, i));
            }
            System.out.println("Length = " + i + " ; sum = " + sum);
            Assert.assertEquals(1., sum, 0.01);
        }
    }

}
