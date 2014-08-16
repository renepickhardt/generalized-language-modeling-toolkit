package de.glmtk.counting.absolute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.Status;
import de.glmtk.counting.Merger;
import de.glmtk.pattern.Pattern;

public class AbsoluteCounter {

    private static final Logger LOGGER = LogManager
            .getLogger(AbsoluteCounter.class);

    private Set<Pattern> neededPatterns;

    private Chunker chunker;

    private Merger merger;

    public AbsoluteCounter(
            Set<Pattern> neededPatterns,
            int numberOfCores,
            int updateInterval) {
        this.neededPatterns = neededPatterns;
        chunker = new Chunker(numberOfCores, updateInterval);
        merger = new Merger(numberOfCores, updateInterval);
    }

    public void
    count(Path inputFile, Path outputDir, Path tmpDir, Status status)
            throws IOException {
        LOGGER.info("Absolute counting '{}' -> '{}'.", inputFile, outputDir);

        Files.createDirectories(outputDir);
        Files.createDirectories(tmpDir);

        Set<Pattern> countingPatterns = new HashSet<Pattern>(neededPatterns);
        countingPatterns.removeAll(status.getAbsoluteCounted());

        Set<Pattern> chunkingPatterns = new HashSet<Pattern>(countingPatterns);
        chunkingPatterns.removeAll(status.getAbsoluteChunkedPatterns());

        LOGGER.info("1/2 Chunking:");
        chunker.chunk(chunkingPatterns, inputFile, tmpDir, status);

        LOGGER.info("2/2 Merging:");
        merger.merge(countingPatterns, tmpDir, outputDir, status);

        LOGGER.info("Absolute counting done.");
    }

}
