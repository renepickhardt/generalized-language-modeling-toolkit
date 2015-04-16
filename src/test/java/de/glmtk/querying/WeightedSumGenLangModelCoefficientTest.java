package de.glmtk.querying;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.glmtk.querying.estimator.weightedsum.WeightedSumGenLangModelEstimator;
import de.glmtk.testutil.LoggingTest;

@RunWith(Parameterized.class)
public class WeightedSumGenLangModelCoefficientTest extends LoggingTest {
    private static Method getCoefficentMethod;

    @BeforeClass
    public static void setUpPrivateMethod() throws NoSuchMethodException, SecurityException {
        getCoefficentMethod = WeightedSumGenLangModelEstimator.class.getDeclaredMethod(
                "getCoefficient", Integer.TYPE, Integer.TYPE);
        getCoefficentMethod.setAccessible(true);
    }

    @Parameters(name = "order = {0}, level = {1}")
    public static Iterable<Object[]> data() {
        List<Object[]> data = new ArrayList<>();

        for (int order = 1; order != 6; ++order)
            for (int level = 0; level != order + 1; ++level)
                data.add(new Object[] {order, level});

        return data;
    }

    private int order;
    private int level;

    public WeightedSumGenLangModelCoefficientTest(int order,
                                                  int level) {
        this.order = order;
        this.level = level;
    }

    @Test
    public void testCoefficient() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        double expected = factorial(order - level) / factorial(order);
        double actual = (double) getCoefficentMethod.invoke(null, order, level);

        assertEquals(expected, actual, 0.001);
    }

    private static double factorial(int n) {
        return n == 0 ? 1.0 : n * factorial(n - 1);
    }
}
