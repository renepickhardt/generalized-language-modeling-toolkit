package de.glmtk.querying.calculator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.querying.ProbMode;
import de.glmtk.querying.calculator.MarkovCalculator;

public class MarkovCalculatorTest {

    @Test
    public void testComputeQueries() throws NoSuchMethodException,
            SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        MarkovCalculator calculator = new MarkovCalculator();
        calculator.setProbMode(ProbMode.MARG);
        calculator.setOrder(4);

        Method computeQueries =
                MarkovCalculator.class.getDeclaredMethod("computeQueries",
                        List.class);
        computeQueries.setAccessible(true);

        // TODO: Write actual test.
        System.out.println(computeQueries.invoke(calculator,
                Arrays.asList("a", "b", "c", "d", "e", "f", "g")));
    }
}
