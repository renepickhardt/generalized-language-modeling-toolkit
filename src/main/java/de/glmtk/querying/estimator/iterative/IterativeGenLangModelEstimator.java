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
import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Counts;
import de.glmtk.counts.Discounts;
import de.glmtk.util.BinomDiamond;
import de.glmtk.util.BinomDiamondNode;

public class IterativeGenLangModelEstimator extends IterativeModKneserNeyEstimator {
    public static class GlmNode extends BinomDiamondNode<GlmNode> {
        private NGram history = null;
        private long absoluteCount = 0;
        private long continuationCount = 0;
        private double gammaNumerator = 0.0;
        private double absoluteFactor = 0.0;
        private double continuationFactor = 0.0;

        @Override
        public String toString() {
            return String.format(
                    "%2d %-9s  abs=%6d  cont=%6d  gamma=%e  absFactor=%e  contFactor=%e",
                    getIndex(), history, absoluteCount, continuationCount,
                    gammaNumerator, absoluteFactor, continuationFactor);
        }
    }

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
        if (history.isEmpty())
            return (double) cache.getAbsolute(sequence) / cache.getNumWords();

        if (!WSKP_NGRAM.concat(
                getFullHistory(sequence, history).convertSkpToWskp()).seen(
                        cache))
            return Double.NaN;

        BinomDiamond<GlmNode> diamond = buildDiamond(history);

        double prob = 0.0;
        for (GlmNode node : diamond) {
            NGram fullSequence = getFullSequence(sequence, node.history);
            if (node.absoluteFactor != 0) {
                double absAlpha = calcAlpha(fullSequence, true,
                        !node.isBottom());
                prob += absAlpha * node.absoluteFactor;
            }
            if (node.continuationFactor != 0) {
                double contAlpha = calcAlpha(fullSequence, false,
                        !node.isBottom());
                prob += contAlpha * node.continuationFactor;
            }
        }

        if (Double.isNaN(prob))
            prob = Double.POSITIVE_INFINITY;
        return prob;
    }

    private BinomDiamond<GlmNode> buildDiamond(NGram history) {
        int order = history.size();
        BinomDiamond<GlmNode> diamond = new BinomDiamond<>(order, GlmNode.class);
        for (GlmNode node : diamond) {
            NGram hist = history.applyIntPattern(~node.getIndex(), order);
            node.history = hist;
            node.absoluteCount = cache.getAbsolute(hist.concat(SKP_NGRAM));
            node.continuationCount = cache.getContinuation(
                    WSKP_NGRAM.concat(hist.convertSkpToWskp()).concat(
                            WSKP_NGRAM)).getOnePlusCount();
            if (node.continuationCount != 0)
                node.gammaNumerator = calcGammaNumerator(hist);
        }

        for (GlmNode node : diamond.inOrder())
            if (node.isTop())
                node.absoluteFactor = 1.0 / node.absoluteCount;
            else {
                double lambdaCoefficient = calcLambdaCoefficient(
                        node.getLevel(), node.getOrder());
                double gammaMult = calcGammaMult(diamond.getTop(), node);
                double denominator = node.continuationCount;

                node.continuationFactor = lambdaCoefficient * gammaMult
                        / denominator;
            }

        return diamond;
    }

    private double calcGammaNumerator(NGram history) {
        Discounts discount = calcDiscounts(history.getPattern().concat(
                PatternElem.CNT));
        Counts contCount = cache.getContinuation(history.concat(WSKP_NGRAM));

        return discount.getOne() * contCount.getOneCount() + discount.getTwo()
                * contCount.getTwoCount() + discount.getThree()
                * contCount.getThreePlusCount();
    }

    private double calcLambdaCoefficient(int level,
                                         int order) {
        int result = 1;
        for (int i = 0; i != level; ++i)
            result *= (order - i);
        return 1.0 / result;
    }

    private double calcGammaMult(GlmNode ancestor,
                                 GlmNode node) {
        double result;
        if (ancestor.isTop())
            result = ancestor.gammaNumerator / ancestor.absoluteCount;
        else
            result = ancestor.gammaNumerator / ancestor.continuationCount;
        double sum = 0.0;
        for (int i = 0; i != ancestor.numChilds(); ++i) {
            GlmNode child = ancestor.getChild(i);
            if (child != node && child.isAncestorOf(node))
                sum += calcGammaMult(child, node);
        }
        if (sum != 0)
            result *= sum;
        return result;
    }
}
