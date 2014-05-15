package de.typology.smoothing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;

public class InterpolatedKneserNeySmoother extends Smoother {

    private Map<Pattern, Map<Integer, Integer>> nGramTimesCountCache =
            new HashMap<Pattern, Map<Integer, Integer>>();

    private Map<Pattern, Double> discountCache = new HashMap<Pattern, Double>();

    public InterpolatedKneserNeySmoother(
            Path absoluteDir,
            Path continuationDir,
            String delimiter) throws IOException {
        super(absoluteDir, continuationDir, delimiter);
    }

    @Override
    protected double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence) {
        System.out.print("  P( " + reqSequence + " | " + condSequence + " )");

        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> sequence = new ArrayList<String>(n);
        sequence.addAll(condSequence);
        sequence.addAll(reqSequence);

        List<String> history = new ArrayList<String>(n);
        history.addAll(condSequence);
        for (int i = 0; i != reqSequence.size(); ++i) {
            history.add(PatternElem.SKIPPED_WORD);
        }

        System.out.println("\t{ sequence = " + sequence + " ; history = "
                + history + " }");

        double sequenceCount = getAbsolute(sequence);
        double historyCount = getAbsolute(history);

        System.out.println("  sequenceCount = " + sequenceCount
                + " ; historyCount = " + historyCount);

        Double result;
        if (sequenceCount == 0) {
            System.out.println("  sequence count is zero");
            result =
                    propabilityCond2(reqSequence,
                            condSequence.subList(1, condSequence.size()));
        } else {
            double discount = discount(getPattern(history));
            result = Math.max(sequenceCount - discount, 0) / historyCount;

            double lambda = lambda_high(history);
            if (lambda != 0) {
                double propbabilityCond2 =
                        propabilityCond2(reqSequence,
                                condSequence.subList(1, condSequence.size()));
                result += lambda * propbabilityCond2;
            }
        }
        System.out.println("  result = " + result);
        return result;
    }

    private double propabilityCond2(
            List<String> reqSequence,
            List<String> condSequence) {
        System.out.print("    P2( " + reqSequence + " | " + condSequence + ")");

        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> sequence = new ArrayList<String>(n);
        sequence.add(PatternElem.SKIPPED_WORD);
        sequence.addAll(condSequence);
        sequence.addAll(reqSequence);

        List<String> history = new ArrayList<String>(n);
        history.add(PatternElem.SKIPPED_WORD);
        history.addAll(condSequence);
        for (int i = 0; i != reqSequence.size(); ++i) {
            history.add(PatternElem.SKIPPED_WORD);
        }

        System.out.println("\t{ sequence = " + sequence + " ; history = "
                + history + " }");

        double sequenceCount = getContinuation(sequence).getOnePlusCount();
        double historyCount = getContinuation(history).getOnePlusCount();

        System.out.println("    sequenceCount = " + sequenceCount
                + " ; historyCount = " + historyCount);

        Double result;
        if (sequenceCount == 0) {
            System.out.println("    sequence count is zero");
            result =
                    propabilityCond2(reqSequence,
                            condSequence.subList(1, condSequence.size()));
        } else {
            double discount = discount(getPattern(history));
            result = Math.max(sequenceCount - discount, 0) / historyCount;

            double lambda = lambda_mid(history);
            if (lambda != 0) {
                double probabilityCond2 =
                        propabilityCond2(reqSequence,
                                condSequence.subList(1, condSequence.size()));
                result += lambda * probabilityCond2;
            }
        }
        System.out.println("    result = " + result);
        return result;
    }

    /**
     * @return The total number of n-grams with {@code pattern} which appear
     *         exactly {@code times} often in the training data.
     */
    private int nGramTimesCount(Pattern pattern, int times) {
        // TODO: check if is getOneCount from ContinuationCounts.
        Map<Integer, Integer> patternCache = nGramTimesCountCache.get(pattern);
        if (patternCache == null) {
            patternCache = new HashMap<Integer, Integer>();
            nGramTimesCountCache.put(pattern, patternCache);
        }

        Integer count = patternCache.get(times);
        if (count == null) {
            count = 0;
            for (int absoluteCount : absoluteCounts.get(pattern).values()) {
                if (absoluteCount == times) {
                    ++count;
                }
            }
            patternCache.put(times, count);
        }

        System.out.println("      nGramTimesCount(" + pattern + "," + times
                + ") = " + count);

        return count;
    }

    /**
     * @return The discount value for n-grams with {@code pattern}.
     */
    private double discount(Pattern pattern) {
        Double discount = discountCache.get(pattern);
        if (discount == null) {
            double n_1 = nGramTimesCount(pattern, 1);
            double n_2 = nGramTimesCount(pattern, 2);
            if (n_1 == 0 && n_2 == 0) {
                discount = 0.;
            } else {
                discount = n_1 / (n_1 + 2. * n_2);
            }
            discountCache.put(pattern, discount);
        }

        System.out.println("    discount(" + pattern + ") = " + discount);

        return discount;
    }

    private double lambda_high(List<String> history) {
        Double result =
                discount(getPattern(history))
                        * getContinuation(history).getOneCount()
                        / getAbsolute(history);
        System.out.println("    lambda_high(" + history + ") = " + "discount("
                + getPattern(history) + ")  * getContinuation(" + history
                + ").getOneCount() / getAbsolute(" + history + ") = " + result);
        return result;
    }

    private double lambda_mid(List<String> history) {
        Double result =
                discount(getPattern(history))
                        * getContinuation(history).getOneCount()
                        / getContinuation(history).getOnePlusCount();
        System.out.println("    lambda_mid(" + history + ") = " + "discount("
                + getPattern(history) + ")  * " + "getContinuation(" + history
                + ").getOneCount() / getContinuation(" + history
                + ").getOnePlusCount() = " + result);
        return result;
    }

}
