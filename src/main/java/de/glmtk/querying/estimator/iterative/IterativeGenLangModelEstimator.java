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

package de.glmtk.querying.estimator.iterative;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;

public class IterativeGenLangModelEstimator extends IterativeModKneserNeyEstimator {
    public IterativeGenLangModelEstimator() {
        super();
        setBackoffMode(BackoffMode.SKP);
    }

    @Override
    public void setBackoffMode(BackoffMode backoffMode) {
        this.backoffMode = backoffMode;
    }

    @Override
    protected double calcProbability(NGram sequence,
                                     NGram history,
                                     int recDepth) {
        if (!getFullHistory(sequence, history).seen(cache))
            return Double.NaN;

        List<NGram> diffHistories = history.getAllDifferentiatedNGrams(backoffMode);
        List<Double> lambdas = calcLambdas(history);

        double prob = 0.0;
        int numDiffHistories = diffHistories.size();
        for (int i = 0; i != numDiffHistories; ++i) {
            double lambda = lambdas.get(i);
            if (lambda != 0) {
                NGram fullSequence = getFullSequence(sequence,
                        diffHistories.get(i));
                double alpha = calcAlpha(fullSequence, i == 0,
                        i != numDiffHistories - 1);
                prob += alpha * lambdas.get(i);
            }
        }

        return prob;
    }

    @Override
    protected List<Double> calcLambdas(NGram history) {
        List<NGram> diffHistories = history.getAllDifferentiatedNGrams(backoffMode);
        int numDiffHistories = diffHistories.size();

        List<Double> lambdas = new ArrayList<>(numDiffHistories);

        for (int i = 0; i != diffHistories.size(); ++i) {
            NGram diffHistory = diffHistories.get(i);

            double lambdaCoeff = calcLambdaCoefficient(diffHistory.getPattern());
            double gammaMult = i == 0 ? 1.0 : calcGammaMult(history,
                    diffHistory);

            long denominator;
            if (i == 0)
                denominator = cache.getAbsolute(diffHistory.concat(SKP_NGRAM));
            else
                denominator = cache.getContinuation(
                        WSKP_NGRAM.concat(diffHistory.convertSkpToWskp()).concat(
                                WSKP_NGRAM)).getOnePlusCount();

            double lambda = lambdaCoeff * gammaMult / denominator;
            lambdas.add(lambda);
        }

        return lambdas;
    }

    private double calcGammaMult(NGram history,
                                 NGram diffHistory) {
        if (history.getPattern().equals(diffHistory.getPattern()))
            return 0.0;

        int numSkips = history.getPattern().numElems(PatternElem.SKP);
        double result = calcGamma(history, numSkips == 0);
        double sum = 0.0;
        for (int i = 0; i != history.size(); ++i)
            if (!history.getWord(i).equals(PatternElem.SKP_WORD)
                    && diffHistory.getWord(i).equals(PatternElem.SKP_WORD)) {
                NGram recHistory = history.set(i, diffHistory.getWord(i));
                sum += calcGammaMult(recHistory, diffHistory);
            }
        if (sum != 0.0)
            result *= sum;

        return result;
    }

    private double calcLambdaCoefficient(Pattern pattern) {
        int order = pattern.size() + 1;
        int numSkips = pattern.numElems(PatternElem.SKP);

        int result = 1;
        for (int i = 0; i != numSkips; ++i)
            result *= (order - i - 1);
        return 1.0 / result;
    }

    protected double calcGamma(NGram history,
                               boolean highestOrder) {
        long denominator;
        if (highestOrder)
            denominator = cache.getAbsolute(history.concat(SKP_NGRAM));
        else
            denominator = cache.getContinuation(
                    WSKP_NGRAM.concat(history.convertSkpToWskp()).concat(
                            WSKP_NGRAM)).getOnePlusCount();

        return calcGamma(history, denominator);
    }
}
