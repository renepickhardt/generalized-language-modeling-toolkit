package de.typology.smoothing.legacy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.typology.patterns.PatternElem;

public class MaximumLikelihoodSmoother extends Smoother {

    private static Logger logger = LoggerFactory
            .getLogger(MaximumLikelihoodSmoother.class);

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

        if (!condSequence.isEmpty()) {
            double conditionCount = getAbsolute(condSequence);
            if (conditionCount == 0) { // otherwise P(reqSequence | condSequence) is not well defined.
                logger.debug("condition count = 0 detected return 0 probability");
                return 0;
            }
        }
        List<String> sequence = getSequence(reqSequence, condSequence);
        List<String> history = getHistory(reqSequence, condSequence);

        double sequenceCount = getAbsolute(sequence);
        double historyCount = getAbsolute(history);

        debugSequenceHistory(sequence, history, sequenceCount, historyCount);

        if (sequenceCount == 0) {
            return calcResultSequenceCount0(reqSequence, condSequence,
                    sequence, history, sequenceCount, historyCount);
        } else {
            return calcResult(reqSequence, condSequence, sequence, history,
                    sequenceCount, historyCount);
        }
    }

    protected double calcResultSequenceCount0(
            List<String> reqSequence,
            List<String> condSequence,
            List<String> sequence,
            List<String> history,
            double sequenceCount,
            double historyCount) {
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
        logger.debug("  P( " + reqSequence + " | " + condSequence + " )");
    }

    protected final void debugSequenceHistory(
            List<String> sequence,
            List<String> history,
            double sequenceCount,
            double historyCount) {
        logger.debug("\t { sequence = " + sequence + " (count = "
                + sequenceCount + ") ; history = " + history + " (count = "
                + historyCount + ") }");
    }

}
