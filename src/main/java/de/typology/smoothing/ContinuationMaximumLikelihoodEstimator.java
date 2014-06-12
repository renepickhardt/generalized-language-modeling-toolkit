package de.typology.smoothing;

import java.util.List;

import de.typology.patterns.PatternElem;

public class ContinuationMaximumLikelihoodEstimator extends
        MaximumLikelihoodEstimator {

    public ContinuationMaximumLikelihoodEstimator(
            Corpus corpus) {
        super(corpus);
    }

    // In order to have continuation counts defined we need to add a skip
    // somewhere. Where? I chose to add a skip at the end of both sequence and
    // history, which seems to pass SumEquals1Test. However for n=5 we would
    // need to have counted our corpus with n=6 for this to work, since else
    // we won't have continuation counts there. Solution?

    @Override
    protected double getNumerator(
            List<String> reqSequence,
            List<String> condSequence) {
        List<String> sequence = getSequence(reqSequence, condSequence);
        sequence.add(0, PatternElem.SKIPPED_WORD);
        double sequenceCount =
                corpus.getContinuation(sequence).getOnePlusCount();
        debugSequence(sequence, sequenceCount);

        return sequenceCount;
    }

    @Override
    protected double getDenominator(
            List<String> reqSequence,
            List<String> condSequence) {
        List<String> history = getHistory(reqSequence, condSequence);
        history.add(0, PatternElem.SKIPPED_WORD);
        double historyCount = corpus.getContinuation(history).getOnePlusCount();
        debugHistory(history, historyCount);

        return historyCount;
    }
}
