package de.glmtk.querying.argmax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import de.glmtk.cache.Cache;
import de.glmtk.cache.CacheBuilder;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Discounts;
import de.glmtk.counts.NGramTimes;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;
import de.glmtk.util.CollectionUtils;
import de.glmtk.util.PeekableIterator;
import de.glmtk.util.StringUtils;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieEntry;

public class TopKMixedAccessArgmaxQueryExecutor implements ArgmaxQueryExecutor {
    private WeightedSumEstimator estimator;
    private Cache cache;
    private Map<Pattern, Discounts> discounts = new HashMap<>();
    private Collection<String> vocab;

    public static CacheBuilder getRequiredCache(CacheBuilder estimatorRequiredCache) {
        return estimatorRequiredCache.withWords();
    }

    public TopKMixedAccessArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                              Collection<String> vocab) {
        this.estimator = estimator;
        cache = estimator.getCache();
        discounts = new HashMap<>();
        this.vocab = vocab;
    }

    public TopKMixedAccessArgmaxQueryExecutor(WeightedSumEstimator estimator) {
        this(estimator, null);
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

            CompletionTrie trie = cache.getCompletionCounts(pattern);
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

            lowestProb = Double.NEGATIVE_INFINITY;
            if (!results.isEmpty())
                lowestProb = results.peek().getProbability();

            String string = entry.getString();
            List<String> split = StringUtils.split(string, ' ');
            String sequence = split.get(split.size() - 1);
            if ((vocab != null && !vocab.contains(sequence))
                    || !seen.add(sequence))
                continue;

            double args[] = new double[size];
            for (int i = 0; i != size; ++i)
                if (i == ptr)
                    args[i] = calcAlpha(histories[i].concat(sequence),
                            entry.getScore());
                else
                    args[i] = calcAlpha(histories[i].concat(sequence));

            double probability = calcProbability(weightedSumFunction, args);

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
        return calcAlpha(sequence, cache.getCount(sequence));
    }

    /**
     * For when we want alpha of sequence we have count for.
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

        Discounts disc = calcDiscounts(sequence.getPattern());
        double d = disc.getForCount(absSequenceCount);

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

        Discounts disc = calcDiscounts(pattern);
        double d = disc.getForCount(count);

        return Math.max(count - d, 0.0);
    }

    protected Discounts calcDiscounts(Pattern pattern) {
        Discounts result = discounts.get(pattern);
        if (result != null)
            return result;

        NGramTimes n = cache.getNGramTimes(pattern);
        double y = (double) n.getOneCount()
                / (n.getOneCount() + n.getTwoCount());
        result = new Discounts(1.0f - 2.0f * y * n.getTwoCount()
                / n.getOneCount(), 2.0f - 3.0f * y * n.getThreeCount()
                / n.getTwoCount(), 3.0f - 4.0f * y * n.getFourCount()
                / n.getThreeCount());

        discounts.put(pattern, result);
        return result;
    }
}
