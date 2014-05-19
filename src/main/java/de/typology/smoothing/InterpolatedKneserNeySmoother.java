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
        /*
         * TODO discuss if it is really corret here to add a skipp at the
         * history (this depends on many things in counting. in general I guess
         * this is correct but for KN smoothing maybe not)
         */
        double historyCount = getAbsolute(history);
        if (historyCount == 0) {
            return 0;
        }

        Double result;
        Pattern historyPattern = getPattern(history);
        if (historyPattern.onlySkp()) {
            result = sequenceCount / historyCount;
        } else {
            double discount = discount(historyPattern);
            /*
             * TODO: lamda is connected to the discount value. why is it not
             * given as a parameter?
             */
            double lambda = lambda_high(history);
            double propbabilityCond2 =
                    propabilityCond2(reqSequence,
                            condSequence.subList(1, condSequence.size()));
            result =
                    Math.max(sequenceCount - discount, 0) / historyCount
                            + lambda * propbabilityCond2;
        }
        //result = result.equals(Double.NaN) ? 0 : result;
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
        if (historyCount == 0) {
            return 0;
        }

        Double result;
        Pattern historyPattern = getPattern(history);
        if (historyPattern.onlySkp()) {
            result = sequenceCount / historyCount;
        } else {
            double discount = discount(historyPattern);
            double lambda = lambda_mid(history);
            double probabilityCond2 =
                    propabilityCond2(reqSequence,
                            condSequence.subList(1, condSequence.size()));
            result =
                    Math.max(sequenceCount - discount, 0) / historyCount
                            + lambda * probabilityCond2;
        }
        //result = result.equals(Double.NaN) ? 0 : result;
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
        if (true) {
            return 0.25;
        }
        Double discount = discountCache.get(pattern);
        if (discount == null) {
            // TODO: tbd. is it really true use pattern instead of history? TBD
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
        //        List<String> history = new ArrayList<String>(history2);
        //        for (int i = history.size() - 1; i != -1; --i) {
        //            if (history.get(i) == PatternElem.SKIPPED_WORD) {
        //                history.remove(i);
        //            } else {
        //                break;
        //            }
        //        }
        //        List<String> skippedHistory = new ArrayList<String>(history2);
        //        for (int i = 0; i != skippedHistory.size(); ++i) {
        //            if (skippedHistory.get(i) != PatternElem.SKIPPED_WORD) {
        //                skippedHistory.set(i, PatternElem.SKIPPED_WORD);
        //                break;
        //            }
        //        }
        int denominator = getAbsolute(history);
        Double result =
                discount(getPattern(history))
                        * getContinuation(history).getOneCount()
                        / getAbsolute(history);
        //result = result.equals(Double.NaN) ? 0 : result;
        if (denominator == 0) {
            System.out.println("####################### denominator 0 for: "
                    + history);
        }
        System.out.println("    lambda_high(" + history + ") = " + "discount("
                + getPattern(history) + ")  * getContinuation(" + history
                + ").getOneCount() / getAbsolute(" + history + ") = " + result);
        return result;
    }

    private double lambda_mid(List<String> history) {
        List<String> skippedHistory = new ArrayList<String>(history);
        for (int i = 0; i != skippedHistory.size(); ++i) {
            if (skippedHistory.get(i) != PatternElem.SKIPPED_WORD) {
                skippedHistory.set(i, PatternElem.SKIPPED_WORD);
                break;
            }
        }

        Double result =
                discount(getPattern(history))
                        * getContinuation(history).getOneCount()
                        / getContinuation(skippedHistory).getOnePlusCount();
        //result = result.equals(Double.NaN) ? 0 : result;
        System.out.println("    lambda_mid(" + history + ") = " + "discount("
                + getPattern(history) + ")  * " + "getContinuation(" + history
                + ").getOneCount() / getContinuation(" + skippedHistory
                + ").getOnePlusCount() = " + result);
        return result;
    }

}
