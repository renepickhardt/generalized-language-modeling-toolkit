package de.glmtk.utils;

import static de.glmtk.utils.PatternElem.CNT;
import static de.glmtk.utils.PatternElem.POS;
import static de.glmtk.utils.PatternElem.PSKP;
import static de.glmtk.utils.PatternElem.WSKP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.glmtk.querying.NGramProbabilityCalculator;
import de.glmtk.querying.ProbMode;
import de.glmtk.querying.estimator.Estimator;

public class PatternCalculator {

    private static class PatternTrackingCountCache extends CountCache {

        private Set<Pattern> usedPatterns = new HashSet<Pattern>();

        private Random random = new Random();

        public PatternTrackingCountCache() throws IOException {
            super(null);
        }

        public Set<Pattern> getUsedPatterns() {
            return usedPatterns;
        }

        @Override
        public long getAbsolute(NGram sequence) {
            usedPatterns.add(sequence.getPattern());
            return random.nextInt(11);
        }

        @Override
        public Counter getContinuation(NGram sequence) {
            usedPatterns.add(sequence.getPattern());
            return new Counter(random.nextInt(11), random.nextInt(11),
                    random.nextInt(11), random.nextInt(11));
        }

        @Override
        public long[] getNGramTimes(Pattern pattern) {
            usedPatterns.add(pattern);
            return new long[] {
                    random.nextInt(10) + 1, random.nextInt(10) + 1,
                    random.nextInt(10) + 1, random.nextInt(10) + 1
            };
        }

    }

    public static Set<Pattern> getUsedPatterns(
            Estimator estimator,
            ProbMode probMode) {
        PatternTrackingCountCache tracker;
        try {
            tracker = new PatternTrackingCountCache();
        } catch (IOException e) {
            // Can't occur.
            throw new RuntimeException(e);
        }

        estimator.setCountCache(tracker);

        NGramProbabilityCalculator calculator =
                new NGramProbabilityCalculator();
        calculator.setEstimator(estimator);
        calculator.setProbMode(probMode);

        List<String> sequence = Arrays.asList("a", "a", "a", "a", "a");
        for (int i = 0; i != 10; ++i) {
            calculator.probability(sequence);
        }

        return tracker.getUsedPatterns();
    }

    public static void addPosPatterns(Set<Pattern> patterns) {
        Set<Pattern> newPatterns = new HashSet<Pattern>();
        for (Pattern pattern : patterns) {
            Pattern curPattern = pattern, lastPattern = null;
            do {
                curPattern = curPattern.replaceLast(CNT, POS);
                newPatterns.add(curPattern);
                newPatterns.add(curPattern.replace(WSKP, PSKP));
                lastPattern = curPattern;
            } while (curPattern != lastPattern);
        }
        patterns.addAll(newPatterns);
    }

    // TODO: untested
    public static Set<Pattern> getCombinations(
            int modelSize,
            List<PatternElem> elems) {
        Set<Pattern> patterns = new HashSet<Pattern>();

        for (int i = 1; i != modelSize + 1; ++i) {
            for (int j = 0; j != pow(elems.size(), i); ++j) {
                List<PatternElem> pattern = new ArrayList<PatternElem>(i);
                int n = j;
                for (int k = 0; k != i; ++k) {
                    pattern.add(elems.get(n % elems.size()));
                    n /= elems.size();
                }
                patterns.add(Pattern.get(pattern));
            }
        }

        return patterns;
    }

    /**
     * Faster than {@link Math#pow(double, double)} because this is only for
     * ints.
     */
    private static int pow(int base, int power) {
        int result = 1;
        for (int i = 0; i != power; ++i) {
            result *= base;
        }
        return result;
    }

}
