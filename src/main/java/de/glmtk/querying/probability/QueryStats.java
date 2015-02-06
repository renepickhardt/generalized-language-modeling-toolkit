/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.querying.probability;

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

    public void addProbability(double probability) {
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

    public void complete() {
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
