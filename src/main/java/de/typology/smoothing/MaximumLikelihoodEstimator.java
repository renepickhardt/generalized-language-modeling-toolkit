package de.typology.smoothing;

import java.util.List;

public class MaximumLikelihoodEstimator extends FractionEstimator {

    @Override
    protected double getNumerator(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        List<String> sequence = getSequence(reqSequence, condSequence);
        double sequenceCount = corpus.getAbsolute(sequence);
        debugSequence(sequence, sequenceCount, recDepth);

        return sequenceCount;
    }

    @Override
    protected double getDenominator(
            List<String> reqSequence,
            List<String> condSequence,
            int recDepth) {
        List<String> history = getHistory(reqSequence, condSequence);
        double historyCount;
        if (history.isEmpty()) {
            historyCount = corpus.getNumWords();
        } else {
            historyCount = corpus.getAbsolute(history);
        }
        debugHistory(history, historyCount, recDepth);

        return historyCount;
    }

}
