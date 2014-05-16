package de.typology.smoothing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.typology.patterns.PatternElem;

public class MaximumLikelihoodSmoother extends Smoother {

    public MaximumLikelihoodSmoother(
            Path absoluteDir,
            Path continuationDir,
            String delimiter) throws IOException {
        super(absoluteDir, continuationDir, delimiter);
    }

    @Override
    protected double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence) {
        debugP(reqSequence, condSequence);

        List<String> sequence = getSequence(reqSequence, condSequence);
        List<String> history = getHistory(reqSequence, condSequence);

        double sequenceCount = getAbsolute(sequence);
        double historyCount = getAbsolute(history);

        debugSequenceHistory(sequence, history, sequenceCount, historyCount);

        if (sequenceCount == 0) {
            return calcResultSequenceCount0(reqSequence, condSequence);
        } else {
            return calcResult(reqSequence, condSequence, sequence, history,
                    sequenceCount, historyCount);
        }
    }

    protected double calcResultSequenceCount0(
            List<String> reqSequence,
            List<String> condSequence) {
        return 0;
    }

    protected double calcResult(
            List<String> reqSequence,
            List<String> condSequence,
            List<String> sequence,
            List<String> history,
            double sequenceCount,
            double historyCount) {
        return sequenceCount / historyCount;
    }

    protected final List<String> getSequence(
            List<String> reqSequence,
            List<String> condSequence) {
        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> sequence = new ArrayList<String>(n);
        sequence.addAll(condSequence);
        sequence.addAll(reqSequence);

        return sequence;
    }

    protected final List<String> getHistory(
            List<String> reqSequence,
            List<String> condSequence) {
        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> history = new ArrayList<String>(n);
        history.addAll(condSequence);
        for (int i = 0; i != reqSequence.size(); ++i) {
            history.add(PatternElem.SKIPPED_WORD);
        }

        return history;
    }

    protected final void debugP(
            List<String> reqSequence,
            List<String> condSequence) {
        if (DEBUG) {
            System.out.print("  P( " + reqSequence + " | " + condSequence
                    + " )");
        }
    }

    protected final void debugSequenceHistory(
            List<String> sequence,
            List<String> history,
            double sequenceCount,
            double historyCount) {
        if (DEBUG) {
            System.out.println("\t { sequence = " + sequence + " (count = "
                    + sequenceCount + ") ; history = " + history + " (count = "
                    + historyCount + ") }");
        }
    }

}
