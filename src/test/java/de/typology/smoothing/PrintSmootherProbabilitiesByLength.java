package de.typology.smoothing;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class PrintSmootherProbabilitiesByLength extends AbcCorpusTest {

    @Test
    @Ignore
    public void printMaximumLikelihoodSmoother() throws IOException {
        Smoother smoother =
                new MaximumLikelihoodSmoother(abcAbsoluteDir,
                        abcContinuationDir, "\t");
        printProbabilitiesByLength(smoother, 5);
    }

    @Test
    public void printDiscountSmoother() throws IOException {
        Smoother smoother =
                new DiscountSmoother(abcAbsoluteDir, abcContinuationDir, "\t",
                        1.);
        printProbabilitiesByLength(smoother, 5);
    }

    @Test
    @Ignore
    public void printInterpolatedKneserNey() throws IOException {
        Smoother smoother =
                new InterpolatedKneserNeySmoother(abcAbsoluteDir,
                        abcContinuationDir, "\t");
        printProbabilitiesByLength(smoother, 5);
    }

    private void printProbabilitiesByLength(Smoother smoother, int length) {
        Map<Integer, Map<String, Double>> propabilitiesByLength =
                new LinkedHashMap<Integer, Map<String, Double>>();
        for (int i = 1; i != length + 1; ++i) {
            propabilitiesByLength
                    .put(i, calcSequencePropabilities(smoother, i));
        }

        for (Map<String, Double> propabilities : propabilitiesByLength.values()) {
            System.out.println("---");
            printPropabilities(propabilities);
        }
    }

    private Map<String, Double> calcSequencePropabilities(
            Smoother smooter,
            int length) {
        Map<String, Double> propabilities = new LinkedHashMap<String, Double>();

        for (int i = 0; i != ((int) Math.pow(3, length)); ++i) {
            String sequence = getAbcSequence(i, length);
            propabilities.put(sequence, smooter.propability(sequence));
        }

        return propabilities;
    }

    private void printPropabilities(Map<String, Double> propabilities) {
        double sum = 0;
        for (Map.Entry<String, Double> sequencePropability : propabilities
                .entrySet()) {
            String sequence = sequencePropability.getKey();
            double propability = sequencePropability.getValue();

            sum += propability;

            System.out.println(sequence + " -> " + propability);
        }
        System.out.println("sum = " + sum);
    }

    private String getAbcSequence(int num, int length) {
        StringBuilder result = new StringBuilder();

        boolean frist = true;
        for (int k = 0; k != length; ++k) {
            if (frist) {
                frist = false;
            } else {
                result.append(" ");
            }
            switch (num % 3) {
                case 0:
                    result.append("a");
                    break;
                case 1:
                    result.append("b");
                    break;
                case 2:
                    result.append("c");
                    break;
            }
            num /= 3;
        }

        return result.reverse().toString();
    }

}
