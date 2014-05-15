package de.typology.smoothing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.typology.patterns.PatternElem;

public class MaximumLikelihoodSmoother extends Smoother {

    public static boolean DEBUG = false;

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
        if (DEBUG) {
            System.out.print("  P( " + reqSequence + " | " + condSequence
                    + " )");
        }

        List<String> sequence = getSequence(reqSequence, condSequence);
        List<String> history = getHistory(reqSequence, condSequence);

        double sequenceCount = getAbsolute(sequence);
        double historyCount = getAbsolute(history);

        if (DEBUG) {
            System.out.println("\t { sequence = " + sequence + " (count = "
                    + sequenceCount + ") ; history = " + history + " (count = "
                    + historyCount + ") }");
        }

        if (sequenceCount == 0) {
            return 0;
        } else {
            return sequenceCount / historyCount;
        }
    }

    protected List<String> getSequence(
            List<String> reqSequence,
            List<String> condSequence) {
        int n = reqSequence.size() + condSequence.size() - 1;

        List<String> sequence = new ArrayList<String>(n);
        sequence.addAll(condSequence);
        sequence.addAll(reqSequence);

        return sequence;
    }

    protected List<String> getHistory(
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

}
