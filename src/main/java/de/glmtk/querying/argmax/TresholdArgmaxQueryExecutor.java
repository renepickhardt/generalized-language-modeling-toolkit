package de.glmtk.querying.argmax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Discounts;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.PeekableIterator;
import de.glmtk.util.StringUtils;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieEntry;

public class TresholdArgmaxQueryExecutor implements ArgmaxQueryExecutor {
    private WeightedSumEstimator estimator;
    private Cache randomAccessCache;
    private CompletionTrieCache sortedAccessCache;
    private Collection<String> vocab;

    public TresholdArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                          Cache randomAccessCache,
                                          CompletionTrieCache sortedAccessCache,
                                          Collection<String> vocab) {
        this.estimator = estimator;
        this.randomAccessCache = randomAccessCache;
        this.sortedAccessCache = sortedAccessCache;
        this.vocab = vocab;
    }

    public TresholdArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                          Cache randomAccessCache,
                                          CompletionTrieCache sortedAccessCache) {
        this(estimator, randomAccessCache, sortedAccessCache, null);
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
        long[] lastCounts = new long[size];

        for (int i = 0; i != size; ++i) {
            Pattern pattern = patterns[i];
            String h = histories[i].toString();
            if (!h.isEmpty())
                h += " ";

            CompletionTrie trie = sortedAccessCache.getCountCompletionTrie(pattern);
            PeekableIterator<CompletionTrieEntry> iter = trie.getCompletions(h);

            tries[i] = trie;
            iters[i] = iter;
            lastCounts[i] = iter.peek().getScore();
        }

        Set<String> seen = new HashSet<>();
        PriorityQueue<ArgmaxResult> results = new PriorityQueue<>(numResults,
                ArgmaxResult.COMPARATOR);

        List<Integer> ptrs = new ArrayList<>(size);
        for (int i = 0; i != size; ++i)
            ptrs.add(i);

        double thresholdArgs[] = new double[size];
        for (int i = 0; i != size; ++i)
            thresholdArgs[i] = calcAlpha(patterns[i], lastCounts[i]);
        double threshold = calcProbability(weightedSumFunction, thresholdArgs);
        double lowestProb = Double.NEGATIVE_INFINITY;

        Iterator<Integer> ptrIter = ptrs.iterator();
        while (!(results.size() == numResults && threshold < lowestProb)) {
            if (!ptrIter.hasNext()) {
                if (ptrs.isEmpty())
                    break;
                ptrIter = ptrs.iterator();
            }
            int ptr = ptrIter.next();

            Iterator<CompletionTrieEntry> iter = iters[ptr];
            if (!iter.hasNext()) {
                ptrIter.remove();
                continue;
            }

            CompletionTrieEntry entry = iter.next();
            threshold -= weightedSumFunction.get(ptr).getWeight()
                    * calcAlpha(patterns[ptr], lastCounts[ptr]);
            lastCounts[ptr] = entry.getScore();
            threshold += weightedSumFunction.get(ptr).getWeight()
                    * calcAlpha(patterns[ptr], lastCounts[ptr]);

            String string = entry.getString();
            List<String> split = StringUtils.split(string, ' ');
            String sequence = split.get(split.size() - 1);
            if (vocab != null && !vocab.contains(sequence))
                continue;
            if (!seen.add(sequence))
                continue;

            double args[] = new double[size];
            for (int i = 0; i != size; ++i)
                if (i == ptr)
                    args[i] = calcAlpha(histories[i].concat(sequence),
                            entry.getScore());
                else
                    args[i] = calcAlpha(histories[i].concat(sequence));

            double probability = calcProbability(weightedSumFunction, args);

            if (probability == 0.0)
                continue;

            if (results.size() < numResults) {
                results.add(new ArgmaxResult(sequence, probability));
                lowestProb = results.peek().getProbability();
            } else if (probability > lowestProb) {
                results.poll();
                results.add(new ArgmaxResult(sequence, probability));
                lowestProb = probability;
            }
        }

        return CollectionUtils.drainQueueToList(results);
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
     * For when we want alpha of random access sequence.
     */
    private double calcAlpha(NGram sequence) {
        return calcAlpha(sequence, randomAccessCache.getCount(sequence));
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
            absSequenceCount = randomAccessCache.getCount(sequence);
        }

        if (sequence.getPattern().numElems(PatternElem.CNT) == 1)
            // If we are on last order don't discount.
            return count;

        Discounts discounts = randomAccessCache.getDiscounts(sequence.getPattern());
        double d = discounts.getForCount(absSequenceCount);

        return Math.max(count - d, 0.0);
    }

    /**
     * For when we want alpha of arbitrary counts with a specific pattern (used
     * for threshold arguments).
     */
    private double calcAlpha(Pattern pattern,
                             long count) {
        if (pattern.numElems(PatternElem.CNT) == 1)
            // If we are on last order don't discount.
            return count;

        Discounts discounts = randomAccessCache.getDiscounts(pattern);
        double d = discounts.getForCount(count);

        return Math.max(count - d, 0.0);
    }
}
