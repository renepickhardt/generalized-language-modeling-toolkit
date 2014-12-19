package de.glmtk.querying.calculator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.querying.ProbMode;
import de.glmtk.querying.calculator.CondCalculator;

public class CondCalculatorTest {

    @Test
    public void testComputQueries() throws NoSuchMethodException,
            SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        CondCalculator calculator = new CondCalculator();
        calculator.setProbMode(ProbMode.MARG);

        Method computeQueries =
                CondCalculator.class.getDeclaredMethod("computeQueries",
                        List.class);
        computeQueries.setAccessible(true);

        // TODO: Write actual test.
        System.out.println(computeQueries.invoke(calculator,
                Arrays.asList("a", "b", "c", "d", "e", "f", "g")));
    }

}
