package de.glmtk;

import java.text.NumberFormat;

public class TestStats {

    private static final double LOG_BASE = Math.log(Constants.LOG_BASE);

    public static final NumberFormat PERCENT_FORMATTER = NumberFormat
            .getPercentInstance();
    static {
        PERCENT_FORMATTER.setMaximumFractionDigits(2);
        PERCENT_FORMATTER.setMinimumFractionDigits(2);
    }

    private int cntZero = 0;

    private int cntNonZero = 0;

    private double sum = 0.0;

    private double entropy = 0.0;

    private double crossEntropy = 0.0;

    public TestStats() {
    }

    public void addProbability(double probability) {
        if (probability == 0) {
            ++cntZero;
        } else {
            double logProbability = Math.log(probability);
            ++cntNonZero;
            sum += probability;
            entropy -= probability * logProbability;
            crossEntropy -= logProbability;
        }
    }

    @Override
    public String toString() {
        if (cntNonZero != 0) {
            entropy /= LOG_BASE;
            crossEntropy /= (cntNonZero * LOG_BASE);
        }

        int cnt = cntZero + cntNonZero;
        if (cnt == 0) {
            // Avoid division by zero.
            cnt = 1;
        }

        StringBuffer result = new StringBuffer();

        result.append("Num Sequences (Prob != 0): ");
        result.append(cntNonZero);
        result.append(" (");
        result.append(PERCENT_FORMATTER.format((double) cntNonZero / cnt));
        result.append(")\n");

        result.append("Num Sequences (Prob == 0): ");
        result.append(cntZero);
        result.append(" (");
        result.append(PERCENT_FORMATTER.format((double) cntZero / cnt));
        result.append(")\n");

        result.append("Sum Probabilities........: ");
        result.append(sum);
        result.append("\n");

        result.append("Entropy..................: ");
        result.append(entropy);
        result.append(" Hart\n");

        result.append("Cross-Entropy............: ");
        result.append(crossEntropy);
        result.append(" Hart\n");

        return result.toString();
    }

}
