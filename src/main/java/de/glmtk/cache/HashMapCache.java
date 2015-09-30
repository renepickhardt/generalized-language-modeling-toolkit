package de.glmtk.cache;

import static de.glmtk.common.Pattern.SKP_PATTERN;
import static de.glmtk.common.PatternElem.WSKP;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.CollectionUtils;


public class HashMapCache extends AbstractCache {
    private static final Logger LOGGER = Logger.get(HashMapCache.class);

    private Map<Pattern, Map<String, Long>> counts = null;
    private Map<Pattern, Map<String, Double>> gammasHigh = null;
    private Map<Pattern, Map<String, Double>> gammasLow = null;

    public HashMapCache(GlmtkPaths paths) {
        super(paths);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Counts //////////////////////////////////////////////////////////////////

    @Override
            /* package */void loadCounts(Collection<Pattern> patterns)
                    throws IOException {
        checkCountPatternsArg(patterns);

        LOGGER.debug("Loading counts for patterns: %s", patterns);

        if (counts == null) {
            counts = new HashMap<>();
        }

        for (Pattern pattern : patterns) {
            if (counts.containsKey(pattern)) {
                continue;
            }

            Map<String, Long> countsForPattern = new HashMap<>();
            counts.put(pattern, countsForPattern);

            Path file = paths.getPatternsFile(pattern);
            try (CountsReader reader =
                new CountsReader(file, Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    countsForPattern.put(reader.getSequence(),
                        reader.getCount());
                }
            }

            if (progressBar != null) {
                progressBar.increase();
            }
        }
    }

    @Override
    public long getCount(NGram ngram) {
        checkNGramArg(ngram);

        Long result = CollectionUtils.getFromNestedMap(counts,
            ngram.getPattern(), ngram.toString(), "Counts not loaded.",
            "Counts with pattern '%s' not loaded.", null);
        return result == null ? 0L : result;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Gammas //////////////////////////////////////////////////////////////////

    @Override
            /* package */void loadGammas(Collection<Pattern> patterns)
                    throws IOException {
        checkGammaPatternsArg(patterns);

        LOGGER.debug("Loading gammas for patterns: %s", patterns);

        if (gammasHigh == null) {
            gammasHigh = new HashMap<>();
        }
        if (gammasLow == null) {
            gammasLow = new HashMap<>();
        }

        for (Pattern pattern : patterns) {
            if (gammasHigh.containsKey(pattern)
                && gammasLow.containsKey(pattern)) {
                continue;
            }

            Map<String, Double> gammasHighForPattern = new HashMap<>();
            gammasHigh.put(pattern, gammasHighForPattern);
            Map<String, Double> gammasLowForPattern = new HashMap<>();
            gammasLow.put(pattern, gammasLowForPattern);

            Path gammasHighFile = paths.getPatternsFile(pattern.concat(WSKP));
            try (CountsReader reader =
                new CountsReader(gammasHighFile, Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String sequence = getGammaHighNGram(reader.getSequence());
                    double gammaHigh = calcGamma(pattern, reader.getCounts());
                    gammasHighForPattern.put(sequence, gammaHigh);
                }
            }

            Path gammasLowFile =
                paths.getPatternsFile(SKP_PATTERN.concat(pattern).concat(WSKP));
            try (CountsReader reader =
                new CountsReader(gammasLowFile, Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String sequence = getGammaLowNGram(reader.getSequence());
                    double gammaLow = calcGamma(pattern, reader.getCounts());
                    gammasLowForPattern.put(sequence, gammaLow);
                }
            }

            if (progressBar != null) {
                progressBar.increase();
            }
        }
    }

    @Override
    public double getGammaHigh(NGram ngram) {
        checkNGramArg(ngram);

        Double result =
            CollectionUtils.getFromNestedMap(gammasHigh, ngram.getPattern(),
                ngram.toString(), "Highest order Gammas not loaded.",
                "Highest order Gammas for pattern '%s' not loaded.", null);
        return result == null ? 0.0 : result;
    }

    @Override
    public double getGammaLow(NGram ngram) {
        checkNGramArg(ngram);

        Double result =
            CollectionUtils.getFromNestedMap(gammasLow, ngram.getPattern(),
                ngram.toString(), "Lower order Gammas not loaded.",
                "Lower order Gammas for pattern '%s' not loaded.", null);
        return result == null ? 0.0 : result;
    }
}
