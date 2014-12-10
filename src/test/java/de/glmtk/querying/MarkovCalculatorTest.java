package de.glmtk.querying;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.querying.calculator.MarkovCalculator;

public class MarkovCalculatorTest {

    @Test
    public void testComputeNGrams() throws NoSuchMethodException,
            SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        MarkovCalculator calculator = new MarkovCalculator();
        calculator.setProbMode(ProbMode.COND);
        calculator.setOrder(4);

        Method computeNGrams =
                MarkovCalculator.class.getDeclaredMethod(
                        "computeSequencesAndHistories", List.class);
        computeNGrams.setAccessible(true);

        // TODO: Write actual test.
        System.out.println(computeNGrams.invoke(calculator,
                Arrays.asList("a", "b", "c", "d", "e", "f", "g")));
    }
}
