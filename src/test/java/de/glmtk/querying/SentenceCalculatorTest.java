package de.glmtk.querying;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.glmtk.querying.calculator.SentenceCalculator;

public class SentenceCalculatorTest {

    @Test
    public void testComputeNGrams() throws NoSuchMethodException,
    SecurityException, IllegalAccessException,
    IllegalArgumentException, InvocationTargetException {
        SentenceCalculator calculator = new SentenceCalculator();
        calculator.setProbMode(ProbMode.MARG);

        Method computeNGrams =
                SentenceCalculator.class.getDeclaredMethod(
                        "computeSequencesAndHistories", List.class);
        computeNGrams.setAccessible(true);

        // TODO: Write actual test.
        System.out.println(computeNGrams.invoke(calculator,
                Arrays.asList("a", "b", "c", "d", "e", "f", "g")));
    }
}
