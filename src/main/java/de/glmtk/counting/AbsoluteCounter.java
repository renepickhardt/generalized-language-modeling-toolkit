package de.glmtk.counting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.pattern.Pattern;

public class AbsoluteCounter {

    private static final Logger LOGGER = LogManager
            .getLogger(AbsoluteCounter.class);

    private Set<Pattern> neededPatterns;

    private AbsoluteChunker chunker;

    private Merger merger;

    public AbsoluteCounter(
            Set<Pattern> neededPatterns,
            int numberOfCores,
            int updateInterval) {
        this.neededPatterns = neededPatterns;
        chunker = new AbsoluteChunker(numberOfCores, updateInterval);
        merger = new Merger(numberOfCores, updateInterval, false);
    }

    public void
        count(Path inputFile, Path countedDir, Path chunkDir, Status status)
                throws IOException {
        LOGGER.info("Absolute counting '{}' -> '{}'.", inputFile, countedDir);

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getCounted(false));

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        chunkingPatterns.removeAll(status.getChunkedPatterns(false));

        LOGGER.info("1/2 Chunking:");
        chunker.chunk(chunkingPatterns, inputFile, chunkDir, status);

        LOGGER.info("2/2 Merging:");
        merger.merge(countingPatterns, chunkDir, countedDir, status);

        LOGGER.info("Absolute counting done.");
    }

}
