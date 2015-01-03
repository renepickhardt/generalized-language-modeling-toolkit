package de.glmtk.querying.calculator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.common.ProbMode;

public class SequenceCalculatorTest {
    @Test
    public void testComputeQueries() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        SequenceCalculator calculator = new SequenceCalculator();
        calculator.setProbMode(ProbMode.MARG);

        Method computeQueries = SequenceCalculator.class.getDeclaredMethod(
                "computeQueries", List.class);
        computeQueries.setAccessible(true);

        // TODO: Write actual test.
        System.out.println(computeQueries.invoke(calculator, Arrays.asList("a",
                "b", "c", "d", "e", "f", "g")));
    }
}
