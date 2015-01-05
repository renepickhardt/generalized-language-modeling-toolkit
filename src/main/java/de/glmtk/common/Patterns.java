package de.glmtk.common;

import static de.glmtk.common.PatternElem.CNT;
import static de.glmtk.common.PatternElem.POS;
import static de.glmtk.common.PatternElem.PSKP;
import static de.glmtk.common.PatternElem.WSKP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.glmtk.querying.calculator.SequenceCalculator;
import de.glmtk.querying.estimator.Estimator;

public class Patterns {
    private static final Map<String, Pattern> AS_STRING_TO_PATTERN = new HashMap<String, Pattern>();

    public static Pattern get() {
        Pattern pattern = AS_STRING_TO_PATTERN.get("");
        if (pattern == null) {
            pattern = new Pattern(new ArrayList<PatternElem>(), "");
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(PatternElem elem) {
        Pattern pattern = AS_STRING_TO_PATTERN.get(elem.toString());
        if (pattern == null) {
            pattern = new Pattern(Arrays.asList(elem), elem.toString());
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(List<PatternElem> elems) {
        StringBuilder asStringBuilder = new StringBuilder();
        for (PatternElem elem : elems)
            asStringBuilder.append(elem.toString());
        String asString = asStringBuilder.toString();

        Pattern pattern = AS_STRING_TO_PATTERN.get(asString);
        if (pattern == null) {
            pattern = new Pattern(elems, asString);
            cachePattern(pattern);
        }
        return pattern;
    }

    public static Pattern get(String asString) {
        Pattern pattern = AS_STRING_TO_PATTERN.get(asString);
        if (pattern == null) {
            List<PatternElem> elems = new ArrayList<PatternElem>(
                    asString.length());
            for (char elemAsChar : asString.toCharArray()) {
                PatternElem elem = PatternElem.fromChar(elemAsChar);
                if (elem == null)
                    throw new RuntimeException(String.format(
                            "Unkown PatternElem: '%s'.", elemAsChar));
                elems.add(elem);
            }

            pattern = new Pattern(elems, asString);
            cachePattern(pattern);
        }
        return pattern;
    }

    private static void cachePattern(Pattern pattern) {
        AS_STRING_TO_PATTERN.put(pattern.toString(), pattern);
    }

    private static class PatternTrackingCountCache extends CountCache {
        private Set<Pattern> usedPatterns = new HashSet<Pattern>();
        private Random random = new Random();

        public PatternTrackingCountCache() throws Exception {
            super(null, null);
        }

        public Set<Pattern> getUsedPatterns() {
            return usedPatterns;
        }

        @Override
        public long getAbsolute(NGram sequence) {
            usedPatterns.add(sequence.getPattern());

            // is it possible that sequence is unseen?
            if (sequence.isEmptyOrOnlySkips())
                return random.nextInt(10) + 1;
            else
                return random.nextInt(11);
        }

        @Override
        public Counter getContinuation(NGram sequence) {
            usedPatterns.add(sequence.getPattern());

            // is it possible that sequence is unseen?
            if (sequence.isEmptyOrOnlySkips())
                return new Counter(random.nextInt(10) + 1,
                        random.nextInt(10) + 1, random.nextInt(10) + 1,
                        random.nextInt(10) + 1);
            else
                return new Counter(random.nextInt(11), random.nextInt(11),
                        random.nextInt(11), random.nextInt(11));
        }

        @Override
        public long[] getNGramTimes(Pattern pattern) {
            usedPatterns.add(pattern);
            return new long[] {random.nextInt(10) + 1, random.nextInt(10) + 1,
                    random.nextInt(10) + 1, random.nextInt(10) + 1};
        }
    }

    public static Set<Pattern> getUsedPatterns(int modelSize,
            Estimator estimator,
            ProbMode probMode) throws Exception {
        PatternTrackingCountCache tracker;
        try {
            tracker = new PatternTrackingCountCache();
        } catch (IOException e) {
            // Can't occur.
            throw new RuntimeException(e);
        }

        estimator.setCountCache(tracker);

        SequenceCalculator calculator = new SequenceCalculator();
        calculator.setEstimator(estimator);
        calculator.setProbMode(probMode);

        for (int n = 0; n != modelSize; ++n) {
            List<String> sequence = new ArrayList<String>(n);
            for (int i = 0; i != n + 1; ++i)
                sequence.add("a");
            for (int i = 0; i != 10; ++i)
                calculator.probability(sequence);
        }

        return tracker.getUsedPatterns();
    }

    // TODO: while loop is almost certainly wrong
    public static Set<Pattern> getPosPatterns(Set<Pattern> patterns) {
        Set<Pattern> result = new HashSet<Pattern>();
        for (Pattern pattern : patterns) {
            Pattern curPattern = pattern, lastPattern = null;
            do {
                curPattern = curPattern.replaceLast(CNT, POS);
                result.add(curPattern);
                result.add(curPattern.replace(WSKP, PSKP));
                lastPattern = curPattern;
            } while (curPattern != lastPattern);
        }
        return result;
    }

    public static Set<Pattern> getCombinations(int modelSize,
            List<PatternElem> elems) {
        Set<Pattern> result = new HashSet<Pattern>();

        for (int i = 1; i != modelSize + 1; ++i)
            for (int j = 0; j != pow(elems.size(), i); ++j) {
                List<PatternElem> pattern = new ArrayList<PatternElem>(i);
                int n = j;
                for (int k = 0; k != i; ++k) {
                    pattern.add(elems.get(n % elems.size()));
                    n /= elems.size();
                }
                result.add(Patterns.get(pattern));
            }

        return result;
    }

    /**
     * Faster than {@link Math#pow(double, double)} because this is only for
     * ints.
     */
    private static int pow(int base,
                           int power) {
        int result = 1;
        for (int i = 0; i != power; ++i)
            result *= base;
        return result;
    }

    public static Map<Integer, Set<Pattern>> groupPatternsBySize(Set<Pattern> patterns) {
        Map<Integer, Set<Pattern>> result = new HashMap<Integer, Set<Pattern>>();
        for (Pattern pattern : patterns) {
            Set<Pattern> patternsWithSize = result.get(pattern.size());
            if (patternsWithSize == null) {
                patternsWithSize = new HashSet<Pattern>();
                result.put(pattern.size(), patternsWithSize);
            }
            patternsWithSize.add(pattern);
        }
        return result;
    }
}
