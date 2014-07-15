package de.glmtk.smoothing.legacy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.patterns.Pattern;

public class InterpolatedKneserNeySmoother extends PropabilityCond2Smoother {

    private static Logger logger = LogManager
            .getLogger(InterpolatedKneserNeySmoother.class);

    private Map<Pattern, Map<Integer, Integer>> nGramTimesCountCache =
            new HashMap<Pattern, Map<Integer, Integer>>();

    private Map<Pattern, Double> discountCache = new HashMap<Pattern, Double>();

    public InterpolatedKneserNeySmoother(
            Path absoluteDir,
            Path continuationDir,
            String delimiter) throws IOException {
        super(absoluteDir, continuationDir, delimiter, 0.);
    }

    @Override
    protected double discount(Pattern pattern) {
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

        logger.debug("    discount(" + pattern + ") = " + discount);

        return discount;
    }

    /**
     * @return The total number of n-grams with {@code pattern} which appear
     *         exactly {@code times} often in the training data.
     */
    protected int nGramTimesCount(Pattern pattern, int times) {
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

        logger.debug("      nGramTimesCount(" + pattern + "," + times + ") = "
                + count);

        return count;
    }

}
