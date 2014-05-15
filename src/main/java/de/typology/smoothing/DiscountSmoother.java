package de.typology.smoothing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import de.typology.patterns.Pattern;
import de.typology.patterns.PatternElem;

public class DiscountSmoother extends MaximumLikelihoodSmoother {

    private double absoluteDiscount;

    public DiscountSmoother(
            Path absoluteDir,
            Path continuationDir,
            String delimiter,
            Double absoluteDiscount) throws IOException {
        super(absoluteDir, continuationDir, delimiter);
        this.absoluteDiscount = absoluteDiscount;
    }

    @Override
    protected double calcResult(
            List<String> reqSequence,
            List<String> condSequence,
            List<String> sequence,
            List<String> history,
            double sequenceCount,
            double historyCount) {
        double discount = discount(getPattern(history));
        double result = Math.max(sequenceCount - discount, 0) / historyCount;

        double lambda = lambda(history, historyCount);
        if (lambda != 0) {
            double probabilityCond2 =
                    calcProbabilityCond2(reqSequence, condSequence);
            result += lambda * probabilityCond2;
        }

        return result;
    }

    protected double calcProbabilityCond2(
            List<String> reqSequence,
            List<String> condSequence) {
        if (condSequence.isEmpty()) {
            List<String> skippedList = new LinkedList<String>();
            skippedList.add(PatternElem.SKIPPED_WORD);
            double vocabSize = getContinuation(skippedList).getOnePlusCount();
            return 1 / vocabSize;
        } else {
            return propabilityCond(reqSequence,
                    calcCondSequence2(condSequence));
        }
    }

    protected List<String> calcCondSequence2(List<String> condSequence) {
        return condSequence.subList(1, condSequence.size());
    }

    protected double discount(Pattern pattern) {
        return absoluteDiscount;
    }

    protected double lambda(List<String> history, double historyCount) {
        return discount(getPattern(history))
                * getContinuation(history).getOneCount() / historyCount;
    }

}
