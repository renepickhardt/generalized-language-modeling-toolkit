package de.glmtk.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public HashMapCache(GlmtkPaths paths) {
        super(paths);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Counts //////////////////////////////////////////////////////////////////

    @Override
    void loadCounts(Collection<Pattern> patterns) throws IOException {
        checkPatternsArg(patterns);

        LOGGER.debug("Loading counts...");

        if (counts == null)
            counts = new HashMap<>();

        for (Pattern pattern : patterns) {
            if (counts.containsKey(pattern))
                continue;

            Map<String, Long> countsForPattern = new HashMap<>();
            counts.put(pattern, countsForPattern);

            Path file = paths.getPatternsFile(pattern);
            try (CountsReader reader = new CountsReader(file, Constants.CHARSET)) {
                while (reader.readLine() != null)
                    countsForPattern.put(reader.getSequence(),
                            reader.getCount());
            }

            if (progress != null)
                progress.increase(1);
        }
    }

    @Override
    public long getCount(NGram ngram) {
        Objects.requireNonNull(ngram);
        if (ngram.isEmpty())
            throw new IllegalArgumentException("Empty ngram.");

        Long result = CollectionUtils.getFromNestedMap(counts,
                ngram.getPattern(), ngram.toString(), "Counts not loaded.",
                "Counts with pattern '%s' not loaded.", null);
        return result == null ? 0L : result;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Gammas //////////////////////////////////////////////////////////////////

    @Override
    void loadGammas(Collection<Pattern> patterns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getGamma(NGram ngram) {
        throw new UnsupportedOperationException();
    }
}
