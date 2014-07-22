package de.glmtk.smoothing.legacy1;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import de.glmtk.patterns.Pattern;

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
    protected double calcResultSequenceCount0(
            List<String> reqSequence,
            List<String> condSequence,
            List<String> sequence,
            List<String> history,
            double sequenceCount,
            double historyCount) {
        double lambda = lambda(history, historyCount);
        if (lambda != 0) {
            if (condSequence.isEmpty()) {
                return lambda / getVocabSize();
            } else {
                double probabilityCond2 =
                        calcProbabilityCond2(reqSequence, condSequence);
                return lambda * probabilityCond2;
            }
        } else {
            return Double.NaN;
        }
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
            if (condSequence.isEmpty()) {
                result += lambda / getVocabSize();
            } else {
                double probabilityCond2 =
                        calcProbabilityCond2(reqSequence, condSequence);
                result += lambda * probabilityCond2;
            }
        }

        return result;
    }

    protected double calcProbabilityCond2(
            List<String> reqSequence,
            List<String> condSequence) {
        return propabilityCond(reqSequence,
                condSequence.subList(1, condSequence.size()));
    }

    protected double discount(Pattern pattern) {
        return absoluteDiscount;
    }

    protected double lambda(List<String> history, double historyCount) {
        return discount(getPattern(history))
                * getContinuation(history).getOneCount() / historyCount;
    }

}
