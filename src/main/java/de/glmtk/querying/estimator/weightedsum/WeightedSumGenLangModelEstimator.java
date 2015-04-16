/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2015 Lukas Schmelzeisen, Rene Pickhardt
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

package de.glmtk.querying.estimator.weightedsum;

import static de.glmtk.common.NGram.SKP_NGRAM;
import static de.glmtk.common.NGram.WSKP_NGRAM;

import java.util.ArrayList;
import java.util.List;

import de.glmtk.common.BackoffMode;
import de.glmtk.common.NGram;
import de.glmtk.util.BinomDiamond;
import de.glmtk.util.BinomDiamondNode;

public class WeightedSumGenLangModelEstimator extends WeightedSumModKneserNeyEstimator {
    public static class GlmNode extends BinomDiamondNode<GlmNode> {
        private NGram history = null;
        private long absoluteCount = 0;
        private long continuationCount = 0;
        private double gammaNumerator = 0.0;
        private double absoluteFactor = 0.0;
        private double continuationFactor = 0.0;
    }

    private static List<List<Double>> coefficients = new ArrayList<>();

    public WeightedSumGenLangModelEstimator() {
        super();
        setBackoffMode(BackoffMode.SKP);
    }

    @Override
    public void setBackoffMode(BackoffMode backoffMode) {
        this.backoffMode = backoffMode;
    }

    @Override
    public WeightedSumFunction calcWeightedSumFunction(NGram history) {
        if (history.isEmpty()) {
            WeightedSumFunction weightedSumFunction = new WeightedSumFunction(1);
            weightedSumFunction.add(1.0 / cache.getNumWords(), history);
            return weightedSumFunction;
        }

        BinomDiamond<GlmNode> diamond = buildGlmDiamond(history);

        WeightedSumFunction weightedSumFunction = new WeightedSumFunction(
                diamond.size());
        for (GlmNode node : diamond) {
            if (node.absoluteFactor != 0)
                weightedSumFunction.add(node.absoluteFactor, node.history);
            if (node.continuationFactor != 0)
                weightedSumFunction.add(node.continuationFactor,
                        WSKP_NGRAM.concat(node.history.convertSkpToWskp()));
        }

        return weightedSumFunction;
    }

    private BinomDiamond<GlmNode> buildGlmDiamond(NGram history) {
        int order = history.size();
        BinomDiamond<GlmNode> diamond = new BinomDiamond<>(order, GlmNode.class);

        for (GlmNode node : diamond.inOrder()) {
            NGram hist = history.applyIntPattern(~node.getIndex(), order);
            node.history = hist;
            node.absoluteCount = cache.getAbsolute(hist.concat(SKP_NGRAM));
            node.continuationCount = cache.getContinuation(
                    WSKP_NGRAM.concat(hist.convertSkpToWskp()).concat(
                            WSKP_NGRAM)).getOnePlusCount();
            node.gammaNumerator = calcGammaNumerator(hist);

            double coeff = getCoefficient(diamond.order(), node.getLevel());

            if (node.absoluteCount == 0)
                node.absoluteFactor = 0.0;
            else if (node.isTop())
                node.absoluteFactor = coeff / node.absoluteCount;
            else {
                node.absoluteFactor = calcAbsoluteFactor(diamond.getTop(), node);
                node.absoluteFactor *= coeff / node.absoluteCount;
            }

            if (node.continuationCount == 0 || node.isTop())
                node.continuationFactor = 0;
            else {
                node.continuationFactor = calcContinuationFactor(
                        diamond.getTop(), node, true);
                node.continuationFactor *= coeff / node.continuationCount;
            }
        }

        return diamond;
    }

    private static double calcCoefficient(int order,
                                          int level) {
        int result = 1;
        for (int i = 0; i != level; ++i)
            result *= (order - i);
        return 1.0 / result;
    }

    private static double getCoefficient(int order,
                                         int level) {
        if (coefficients.size() <= order)
            for (int o = coefficients.size(); o != order + 1; ++o) {
                List<Double> coeffs = new ArrayList<>();
                for (int l = 0; l != o + 1; ++l)
                    coeffs.add(calcCoefficient(o, l));
                coefficients.add(coeffs);
            }

        return coefficients.get(order).get(level);
    }

    private int calcAbsoluteFactor(GlmNode ancestor,
                                   GlmNode node) {
        if (ancestor.absoluteCount != 0)
            return 0;

        if (ancestor.getLevel() == node.getLevel() - 1)
            return 1;

        int numUnseenPaths = 0;
        for (int i = 0; i != ancestor.numChilds(); ++i) {
            GlmNode child = ancestor.getChild(i);
            if (child.isAncestorOf(node))
                numUnseenPaths += calcAbsoluteFactor(child, node);
        }

        return numUnseenPaths;
    }

    private double calcContinuationFactor(GlmNode ancestor,
                                          GlmNode node,
                                          boolean absolute) {
        boolean last = ancestor.getLevel() == node.getLevel() - 1;
        double mult = 1.0;
        if (absolute)
            if (ancestor.absoluteCount == 0) {
                if (last && node.absoluteFactor != 0)
                    return 0;
            } else {
                mult = ancestor.gammaNumerator / ancestor.absoluteCount;
                absolute = false;
            }
        else if (ancestor.continuationCount != 0)
            mult = ancestor.gammaNumerator / ancestor.continuationCount;

        if (last)
            return mult;

        double sum = 0;
        for (int i = 0; i != ancestor.numChilds(); ++i) {
            GlmNode child = ancestor.getChild(i);
            if (child.isAncestorOf(node))
                sum += calcContinuationFactor(child, node, absolute);
        }

        return mult * sum;
    }
}
