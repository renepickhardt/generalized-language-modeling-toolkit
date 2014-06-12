package de.typology.smoothing;

import java.util.List;

import de.typology.patterns.PatternElem;

public abstract class FractionEstimator extends Estimator {

    public enum BackoffCalc {
        UNIGRAM_ABSOLUTE,

        UNIGRAM_CONTINUATION,

        UNIFORM_DISTRIBUTION
    }

    protected BackoffCalc backoffCalc;

    public FractionEstimator(
            Corpus corpus) {
        super(corpus);
        backoffCalc = BackoffCalc.UNIGRAM_ABSOLUTE;
    }

    @Override
    public double propabilityCond(
            List<String> reqSequence,
            List<String> condSequence) {
        debugPropabilityCond(reqSequence, condSequence);

        double result;
        // TODO: check if works with continuation counter mle
        if (!condSequence.isEmpty() && corpus.getAbsolute(condSequence) == 0) {
            // Pcond(reqSequence | condSequence) is not well defined.
            logger.debug("    condSequenceCount = 0");
            result = substituePropability(reqSequence);
        } else {
            double denominator = getDenominator(reqSequence, condSequence);
            // TODO: Rene: check if this is formally correct
            if (denominator == 0) {
                logger.debug("    denominator = 0");
                result = substituePropability(reqSequence);
            } else {
                double numerator = getNumerator(reqSequence, condSequence);
                result = numerator / denominator;
            }
        }

        logger.debug("    result = " + result);
        return result;
    }

    protected double substituePropability(List<String> reqSequence) {
        switch (backoffCalc) {
            case UNIGRAM_ABSOLUTE:
                logger.debug("    returning unigram distribution (absolute)");

                // I can't get unigram distribution right, SumEquals1Test for
                // DeleteMLE n=5 always fails.

                // For P( x _ | y ) if y is not seen we wanted to return unigram
                // distribution of "x". However I think unigram distribution of
                // "x _" would be better, this is why i use "reqSequence", if
                // you only want "x" use "reqSequence.subList(0, 1)" instead.

                // This is how I think it should be, why does it fail?
                return corpus.getAbsolute(reqSequence.subList(0, 1)) * 1.0
                        / corpus.getNumWords();

                // This shouldn't work but does for anything but DeleteMLE n=5,
                // but it is called for the other cases, why is is working there?
                //return corpus.getAbsolute(reqSequence) / corpus.getVocabSize();

            case UNIGRAM_CONTINUATION:
                logger.debug("    returning unigram distribution (continuation)");

                // I don't even know of to implement this. Were would we place
                // the skip in order to have continuation counts defined?
                // Similar problem arises for ContinuationMaximumLikelihood:
                // were do we place the skip to have ContCounts defined?
                // If we add it at the end DeleteMLE n=5 fails.
                reqSequence.add(0, PatternElem.SKIPPED_WORD);
                return corpus.getContinuation(reqSequence).getOnePlusCount()
                        / corpus.getVocabSize();

            default:
            case UNIFORM_DISTRIBUTION:
                logger.debug("    returning uniform distribution (1/vocabSize)");
                return 1.0 / corpus.getVocabSize();
        }
    }

    protected abstract double getNumerator(
            List<String> reqSequence,
            List<String> condSequence);

    protected abstract double getDenominator(
            List<String> reqSequence,
            List<String> condSequence);

}
