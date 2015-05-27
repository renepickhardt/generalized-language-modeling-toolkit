/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2015 Lukas Schmelzeisen
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

import static de.glmtk.output.Output.printlnWarning;

import java.io.IOException;
import java.util.List;

import de.glmtk.GlmtkPaths;
import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheSpecification;
import de.glmtk.querying.calculator.Calculator;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.StringUtils;

public class QueryExecutor {
    private QueryMode mode;
    private int corpusOrder;
    private QueryStats stats;
    private Calculator calculator;
    private Cache cache;

    public QueryExecutor(GlmtkPaths paths,
                         QueryMode mode,
                         Estimator estimator,
                         int corpusOrder) throws IOException {
        this.mode = mode;
        this.corpusOrder = corpusOrder;

        stats = new QueryStats();

        calculator = Calculator.forQueryMode(mode);
        calculator.setEstimator(estimator);

        if (mode.isWithLengthFreq())
            cache = new CacheSpecification().withLengthDistribution().build(
                    paths);
    }

    /**
     * Sideeffect: Restarts counting stats after this call.
     */
    public QueryStats getResultingStats() {
        QueryStats result = stats;
        result.complete();

        stats = new QueryStats();

        return result;
    }

    public double querySequence(String sequence) {
        List<String> tokens = StringUtils.split(sequence, ' ');
        int order = tokens.size();
        Integer modeOrder = mode.getOrder();

        if (order == 0)
            return Double.NaN;
        else if (modeOrder != null && order != modeOrder)
            throw new IllegalStateException(
                    String.format(
                            "Illegal sequence. Can only query sequences with length %d when using mode '%s'.",
                            modeOrder, mode));
        else if (order > corpusOrder)
            throw new IllegalStateException(
                    String.format(
                            "Illegal sequence. Can only query sequences with max length learned on corpus %d.",
                            corpusOrder));

        double prob = calculator.probability(tokens);
        if (mode.isWithLengthFreq() && prob != 0)
            prob *= cache.getLengthFrequency(order);

        synchronized (stats) {
            stats.addProbability(prob);
        }

        return prob;
    }

    public String queryLine(String line) {
        return queryLine(line, null);
    }

    public String queryLine(String line,
                            Integer lineNo) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.charAt(0) == '#')
            return line;

        Double prob = null;
        try {
            prob = querySequence(line);
        } catch (IllegalStateException e) {
            if (lineNo == null)
                printlnWarning(e.getMessage());
            else
                printlnWarning("%s Line %d: '%s'.", e.getMessage(), lineNo,
                        line);
        }

        if (prob == null)
            return line;
        return line + '\t' + prob.toString();
    }
}
