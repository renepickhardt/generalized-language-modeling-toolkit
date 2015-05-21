package de.glmtk.querying.argmax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import de.glmtk.cache.CompletionTrieCache;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.counts.Discounts;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.querying.estimator.weightedsum.WeightedSumEstimator;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction;
import de.glmtk.querying.estimator.weightedsum.WeightedSumFunction.Summand;
import de.glmtk.util.PeekableIterator;
import de.glmtk.util.StringUtils;
import de.glmtk.util.completiontrie.CompletionTrie;
import de.glmtk.util.completiontrie.CompletionTrieEntry;

public class NoRandomAccessArgmaxQueryExecutor implements ArgmaxQueryExecutor {
    public enum ProbbabilityDislay {
        EXACT,

        AVERAGE,

        LOWER_BOUND,

        UPPER_BOUND
    }

    private WeightedSumEstimator estimator;
    private CompletionTrieCache cache;
    private Collection<String> vocab;
    private ProbbabilityDislay probbabilityDislay;

    private static class ArgmaxObject {
        public static final Comparator<ArgmaxObject> COMPARATOR = new Comparator<ArgmaxObject>() {
            @Override
            public int compare(ArgmaxObject lhs,
                               ArgmaxObject rhs) {
                int cmp = -Double.compare(lhs.lowerBound, rhs.lowerBound);
                if (cmp != 0)
                    return cmp;

                return -Double.compare(rhs.upperBound, rhs.upperBound);
            }
        };

        public String sequence;
        public double upperBound, lowerBound;
        public long[] counts;
        public boolean done;

        @Override
        public int hashCode() {
            return sequence.hashCode();
        }
    }

    public NoRandomAccessArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                           CompletionTrieCache cache,
                                           Collection<String> vocab) {
        this.estimator = estimator;
        this.cache = cache;
        this.vocab = vocab;
        probbabilityDislay = ProbbabilityDislay.AVERAGE;
    }

    public NoRandomAccessArgmaxQueryExecutor(WeightedSumEstimator estimator,
                                           CompletionTrieCache cache) {
        this(estimator, cache, null);
    }

    public ProbbabilityDislay getProbbabilityDislay() {
        return probbabilityDislay;
    }

    public void setProbbabilityDislay(ProbbabilityDislay probbabilityDislay) {
        this.probbabilityDislay = probbabilityDislay;
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

            CompletionTrie trie = cache.getCountCompletionTrie(pattern);
            PeekableIterator<CompletionTrieEntry> iter = trie.getCompletions(h);

            tries[i] = trie;
            iters[i] = iter;
            lastCounts[i] = iter.peek().getScore();
        }

        Map<String, ArgmaxObject> objects = new HashMap<>();
        PriorityQueue<ArgmaxObject> queue = new PriorityQueue<>(11,
                ArgmaxObject.COMPARATOR);
        List<ArgmaxResult> results = new ArrayList<>(numResults);

        List<Integer> ptrs = new ArrayList<>(size);
        for (int i = 0; i != size; ++i)
            ptrs.add(i);

        Iterator<Integer> ptrIter = ptrs.iterator();
        while (results.size() != numResults) {
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
            lastCounts[ptr] = entry.getScore();

            String string = entry.getString();
            List<String> split = StringUtils.split(string, ' ');
            String sequence = split.get(split.size() - 1);
            if (vocab != null && !vocab.contains(sequence))
                continue;

            ArgmaxObject object = objects.get(sequence);
            if (object == null) {
                object = new ArgmaxObject();
                objects.put(sequence, object);
                object.sequence = sequence;
                object.done = false;
                object.counts = new long[size];
                for (int i = 0; i != size; ++i)
                    object.counts[i] = 0;
            } else if (object.done)
                continue;
            else
                queue.remove(object);
            object.counts[ptr] = entry.getScore();

            double upperBoundArgs[] = new double[size];
            double lowerBoundArgs[] = new double[size];
            for (int i = 0; i != size; ++i)
                if (object.counts[i] == 0) {
                    upperBoundArgs[i] = calcAlpha(
                            histories[i].concat(sequence), lastCounts[i]);
                    lowerBoundArgs[i] = 0;
                } else
                    upperBoundArgs[i] = lowerBoundArgs[i] = calcAlpha(
                            histories[i].concat(sequence), object.counts[i]);
            object.upperBound = calcProbability(weightedSumFunction,
                    upperBoundArgs);
            object.lowerBound = calcProbability(weightedSumFunction,
                    lowerBoundArgs);

            queue.add(object);

            while (true) {
                object = queue.remove();
                if (!queue.isEmpty()
                        && object.lowerBound >= queue.peek().upperBound) {
                    results.add(new ArgmaxResult(object.sequence,
                            calcDisplayProbability(weightedSumFunction,
                                    histories, object)));
                    object.done = true;
                } else {
                    queue.add(object);
                    break;
                }
            }
        }

        for (int i = results.size(); i < numResults; ++i) {
            if (queue.isEmpty())
                break;
            ArgmaxObject object = queue.remove();
            if (object.upperBound == 0.0)
                break;
            results.add(new ArgmaxResult(object.sequence,
                    calcDisplayProbability(weightedSumFunction, histories,
                            object)));
        }

        return results;
    }

    private double calcDisplayProbability(WeightedSumFunction weightedSumFunction,
                                          NGram[] histories,
                                          ArgmaxObject object) {
        switch (probbabilityDislay) {
            case EXACT:
                int size = histories.length;
                double args[] = new double[size];
                for (int i = 0; i != size; ++i)
                    if (object.counts[i] == 0)
                        args[i] = calcAlpha(histories[i].concat(object.sequence));
                    else
                        args[i] = calcAlpha(
                                histories[i].concat(object.sequence),
                                object.counts[i]);
                return calcProbability(weightedSumFunction, args);
            case AVERAGE:
                return (object.lowerBound + object.upperBound) / 2;
            case LOWER_BOUND:
                return object.lowerBound;
            case UPPER_BOUND:
                return object.upperBound;
            default:
                throw new SwitchCaseNotImplementedException();
        }
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

    /**
     * For when we want alpha of arbitrary counts with a specific pattern (used
     * for threshold arguments).
     */
    private double calcAlpha(Pattern pattern,
                             long count) {
        if (pattern.numElems(PatternElem.CNT) == 1)
            // If we are on last order don't discount.
            return count;

        Discounts discounts = cache.getDiscounts(pattern);
        double d = discounts.getForCount(count);

        return Math.max(count - d, 0.0);
    }
}
