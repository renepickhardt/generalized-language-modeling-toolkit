package de.glmtk.querying.argmax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Discounts;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;
import de.glmtk.util.StringUtils;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieEntry;

public class BeamSearchArgmaxQueryExecutor implements ArgmaxQueryExecutor {
    private WeightedSumEstimator estimator;
    private CompletionTrieCache cache;
    private Collection<String> vocab;

    public BeamSearchArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                         CompletionTrieCache cache,
                                         Collection<String> vocab) {
        this.estimator = estimator;
        this.cache = cache;
        this.vocab = vocab;
    }

    public BeamSearchArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                         CompletionTrieCache cache) {
        this(estimator, cache, null);
    }

    @Override
    public List<ArgmaxResult> queryArgmax(String history,
                                          int numResults) {
        if (numResults == 0)
            return new ArrayList<>();
        if (numResults < 0)
            throw new IllegalArgumentException("numResults must be positive.");

        NGram hist = new NGram(StringUtils.split(history, ' '));
        WeightedSumFunction weightedSumFunction = estimator.calcWeightedSumFunction(hist);

        int size = weightedSumFunction.size();
        if (size == 0)
            // TODO: what to do here?
            return new ArrayList<>();

        Pattern[] patterns = weightedSumFunction.getPatterns();
        NGram[] histories = weightedSumFunction.getHistories();
        CompletionTrie[] tries = new CompletionTrie[size];
        @SuppressWarnings("unchecked")
        Iterator<CompletionTrieEntry>[] iters = new Iterator[size];

        for (int i = 0; i != size; ++i) {
            Pattern pattern = patterns[i];
            String h = histories[i].toString();
            if (!h.isEmpty())
                h += " ";

            CompletionTrie trie = cache.getCountCompletionTrie(pattern);
            Iterator<CompletionTrieEntry> iter = trie.getCompletions(h);

            tries[i] = trie;
            iters[i] = iter;
        }

        return null;
    }

    private double calcProbability(WeightedSumFunction weightedSumFunction,
                                   double[] args) {
        double prob = 0;

        int i = 0;
        for (Summand summand : weightedSumFunction) {
            prob += summand.getWeight() * args[i];
            ++i;
        }

        return prob;
    }

    /**
     * For when we want alpha of sequence we have the count for.
     */
    private double calcAlpha(NGram sequence,
                             long count) {
        if (count == 0)
            return 0;

        long absSequenceCount = count;

        if (!sequence.getPattern().isAbsolute()) {
            sequence = sequence.remove(0).convertWskpToSkp();
            absSequenceCount = cache.getCount(sequence);
        }

        if (sequence.getPattern().numElems(PatternElem.CNT) == 1)
            // If we are on last order don't discount.
            return count;

        Discounts discounts = cache.getDiscounts(sequence.getPattern());
        double d = discounts.getForCount(absSequenceCount);

        return Math.max(count - d, 0.0);
    }
}
