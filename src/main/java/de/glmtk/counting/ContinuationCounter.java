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

    private ContinuationChunker chunker;

    private Merger merger;

    public ContinuationCounter(
            Set<Pattern> neededPatterns,
            int numberOfCores,
            int updateInterval) {
        this.neededPatterns = neededPatterns;
        chunker = new ContinuationChunker(numberOfCores, updateInterval);
        merger = new Merger(numberOfCores, updateInterval, true);
    }

    public void count(
            Status status,
            Path absoluteCountedDir,
            Path absoluteChunkedDir,
            Path continuationCountedDir,
            Path continuationChunkedDir) throws IOException {
        LOGGER.info("Continuation counting '{}' -> '{}'.", absoluteCountedDir,
                continuationCountedDir);

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(true));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(true));

        LOGGER.info("1/2 Chunking:");
        chunker.chunk(status, chunkingPatterns, absoluteCountedDir,
                absoluteChunkedDir, continuationCountedDir, continuationChunkedDir);

        LOGGER.info("2/2 Merging:");
        merger.merge(status, countingPatterns,
                continuationChunkedDir, continuationCountedDir);

        LOGGER.info("Continuation counting done.");
    }

}
