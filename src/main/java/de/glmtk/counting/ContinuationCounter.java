package de.glmtk.counting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.ConsoleOutputter;
import de.glmtk.ConsoleOutputter.Phase;
import de.glmtk.Status;
import de.glmtk.utils.Pattern;

public class ContinuationCounter {

    // TODO: Check why so many cont couts end with 0\t0.

    private static final Logger LOGGER = LogManager
            .getLogger(ContinuationCounter.class);

    private Set<Pattern> neededPatterns;

    private ContinuationChunker chunker;

    private Merger merger;

    public ContinuationCounter(
            Set<Pattern> neededPatterns,
            int numberOfCores,
            int consoleUpdateInterval,
            int logUpdateInterval) {
        this.neededPatterns = neededPatterns;
        chunker =
                new ContinuationChunker(numberOfCores, consoleUpdateInterval,
                        logUpdateInterval);
        merger =
                new Merger(numberOfCores, consoleUpdateInterval,
                        logUpdateInterval, true);
    }

    public void count(
            ConsoleOutputter consolueOutputter,
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
        consolueOutputter.setPhase(Phase.CONTINUATION_CHUNKING, -1.0);
        chunker.chunk(consolueOutputter, status, chunkingPatterns,
                absoluteCountedDir, absoluteChunkedDir, continuationCountedDir,
                continuationChunkedDir);

        LOGGER.info("2/2 Merging:");
        consolueOutputter.setPhase(Phase.CONTINUATION_MERGING, -1.0);
        merger.merge(consolueOutputter, status, countingPatterns,
                continuationChunkedDir, continuationCountedDir);

        LOGGER.info("Continuation counting done.");
    }

}
