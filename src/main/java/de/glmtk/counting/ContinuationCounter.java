package de.glmtk.counting;

import static de.glmtk.common.Console.CONSOLE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.common.Console.Phase;
import de.glmtk.common.Pattern;

public class ContinuationCounter {

    // TODO: Check why so many cont couts end with 0\t0.

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(ContinuationCounter.class);

    private Set<Pattern> neededPatterns;

    private ContinuationChunker chunker;

    private Merger merger;

    public ContinuationCounter(
            Set<Pattern> neededPatterns,
            int numberOfCores,
            int consoleUpdateInterval,
            int logUpdateInterval) {
        this.neededPatterns = neededPatterns;
        chunker = new ContinuationChunker();
        merger = new Merger(true);
    }

    public void count(
            Status status,
            Path absoluteCountedDir,
            Path absoluteChunkedDir,
            Path continuationCountedDir,
            Path continuationChunkedDir) throws IOException {
        LOGGER.info("Continuation counting '%s' -> '%s'.", absoluteCountedDir,
                continuationCountedDir);

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(true));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(true));

        LOGGER.info("1/2 Chunking:");
        CONSOLE.setPhase(Phase.CONTINUATION_CHUNKING);
        chunker.chunk(status, chunkingPatterns, absoluteCountedDir,
                absoluteChunkedDir, continuationCountedDir,
                continuationChunkedDir);

        LOGGER.info("2/2 Merging:");
        CONSOLE.setPhase(Phase.CONTINUATION_MERGING);
        merger.merge(status, countingPatterns, continuationChunkedDir,
                continuationCountedDir);

        LOGGER.info("Continuation counting done.");
    }

}
