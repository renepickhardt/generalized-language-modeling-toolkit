package de.glmtk.counting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.pattern.Pattern;

public class ContinuationCounter {

    private static final Logger LOGGER = LogManager
            .getLogger(ContinuationCounter.class);

    private Set<Pattern> neededPatterns;

    private ContinuationChunker continuationChunker;

    private Merger merger;

    public ContinuationCounter(
            Set<Pattern> neededPatterns,
            int numberOfCores,
            int updateInterval) {
        this.neededPatterns = neededPatterns;
        continuationChunker =
                new ContinuationChunker(numberOfCores, updateInterval);
        merger = new Merger(numberOfCores, updateInterval, true);
    }

    public void
        count(Path inputDir, Path outputDir, Path tmpDir, Status status)
            throws IOException {
        LOGGER.info("Continuation counting '{}' -> '{}'.", inputDir, outputDir);

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(true));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(true));

        LOGGER.info("1/2 Chunking:");

        LOGGER.info("2/2 Merging:");

        LOGGER.info("Continuation couting done.");
    }

}
