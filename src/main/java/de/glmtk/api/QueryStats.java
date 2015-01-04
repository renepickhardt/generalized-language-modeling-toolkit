package de.glmtk.api;

import java.text.NumberFormat;
import java.util.Formatter;

import de.glmtk.Constants;

public class QueryStats {
    private int cntZero = 0;
    private int cntNonZero = 0;
    private double sum = 0.0;
    private double entropy = 0.0;
    private double crossEntropy = 0.0;
    private String string = null;

    /* package */void addProbability(double probability) {
        if (probability == 0)
            ++cntZero;
        else {
            double logProbability = Math.log(probability);
            ++cntNonZero;
            sum += probability;
            entropy -= probability * logProbability;
            crossEntropy -= logProbability;
        }
    }

    /* package */void complete() {
        if (cntNonZero != 0) {
            double baseLog = Math.log(Constants.LOG_BASE);
            entropy /= baseLog;
            crossEntropy /= (cntNonZero * baseLog);
        }
        string = generateString();
    }

    private String generateString() {
        int cnt = cntZero + cntNonZero;
        if (cnt == 0)
            // Avoid division by zero.
            cnt = 1;

        NumberFormat percentFormatter = NumberFormat.getPercentInstance();
        percentFormatter.setMaximumFractionDigits(2);
        percentFormatter.setMinimumFractionDigits(2);

        try (Formatter f = new Formatter()) {
            f.format("Num Sequences (Prob != 0): %d (%.2f)\n", cntNonZero,
                    100.0 * cntNonZero / cnt);
            f.format("Num Sequences (Prob == 0): %d (%.2f)\n", cntZero, 100.0
                    * cntZero / cnt);
            f.format("Sum Probabilities........: %f\n", sum);
            f.format("Entropy..................: %f %s\n", entropy,
                    getEntropyUnit(Constants.LOG_BASE));
            f.format("Cross-Entropy............: %f %s\n", crossEntropy,
                    getEntropyUnit(Constants.LOG_BASE));
            return f.toString();
        }
    }

    @Override
    public String toString() {
        return string;
    }

    public static String getEntropyUnit(double logBase) {
        if (logBase == 2.0)
            return "Sh";
        else if (logBase == 10.0)
            return "Hart";
        else if (logBase == Math.E)
            return "nat";
        else
            return "";
    }
}
