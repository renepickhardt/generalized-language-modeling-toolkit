package de.glmtk.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.glmtk.Constants;
import de.glmtk.GlmtkPaths;
import de.glmtk.common.NGram;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.files.CountsReader;
import de.glmtk.logging.Logger;
import de.glmtk.util.CollectionUtils;

public class HashMapCache extends AbstractCache {
    private static final Logger LOGGER = Logger.get(HashMapCache.class);

    private Map<Pattern, Map<String, Long>> counts = null;
    private Map<Pattern, Map<String, Double>> gammas = null;

    public HashMapCache(GlmtkPaths paths) {
        super(paths);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Counts //////////////////////////////////////////////////////////////////

    @Override
    void loadCounts(Collection<Pattern> patterns) throws IOException {
        checkCountPatternsArg(patterns);

        LOGGER.debug("Loading counts for patterns: %s", patterns);

        if (counts == null)
            counts = new HashMap<>();

        for (Pattern pattern : patterns) {
            if (counts.containsKey(pattern))
                continue;

            Map<String, Long> countsForPattern = new HashMap<>();
            counts.put(pattern, countsForPattern);

            Path file = paths.getPatternsFile(pattern);
            try (CountsReader reader = new CountsReader(file,
                    Constants.CHARSET)) {
                while (reader.readLine() != null)
                    countsForPattern.put(reader.getSequence(),
                            reader.getCount());
            }

            if (progressBar != null)
                progressBar.increase();
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
    void loadGammas(Collection<Pattern> patterns) throws IOException {
        checkGammaPatternsArg(patterns);

        LOGGER.debug("Loading gammas for patterns: %s", patterns);

        if (gammas == null)
            gammas = new HashMap<>();

        for (Pattern pattern : patterns) {
            if (gammas.containsKey(pattern))
                continue;

            Map<String, Double> gammasForPattern = new HashMap<>();
            gammas.put(pattern, gammasForPattern);

            Path file = paths.getPatternsFile(pattern.concat(PatternElem.WSKP));
            try (CountsReader reader = new CountsReader(file,
                    Constants.CHARSET)) {
                while (reader.readLine() != null) {
                    String sequence = removeTrailingWSkp(reader.getSequence());
                    double gamma = calcGamma(pattern, reader.getCounts());
                    gammasForPattern.put(sequence, gamma);
                }
            }

            if (progressBar != null)
                progressBar.increase();
        }
    }

    @Override
    public double getGamma(NGram ngram) {
        checkNGramArg(ngram);

        Double result = CollectionUtils.getFromNestedMap(gammas,
                ngram.getPattern(), ngram.toString(), "Gammas not loaded.",
                "Gammas with pattern '%s' not loaded.", null);
        return result == null ? 0.0 : result;
    }
}
