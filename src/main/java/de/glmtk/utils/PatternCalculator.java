package de.glmtk.utils;

import java.io.IOException;
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

}
