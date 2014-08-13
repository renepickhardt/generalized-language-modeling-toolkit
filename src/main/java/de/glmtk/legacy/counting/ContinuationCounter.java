package de.glmtk.legacy.counting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.legacy.indexing.Index;
import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;

/**
 * Counts continuation counts of sequences for a number of patterns using
 * absolute counts.
 */
public class ContinuationCounter {

    private static Logger logger = LogManager
            .getLogger(ContinuationCounter.class);

    private Path inputDir;

    private Path outputDir;

    private Index wordIndex;

    private String delimiter;

    private int numberOfCores;

    @SuppressWarnings("unused")
    private boolean withPos;

    private boolean sortCounts;

    public ContinuationCounter(
            Path inputDir,
            Path outputDir,
            Index wordIndex,
            String delimiter,
            int numberOfCores,
            boolean withPos,
            boolean sortCounts) throws IOException {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.wordIndex = wordIndex;
        this.delimiter = delimiter;
        this.numberOfCores = numberOfCores;
        this.withPos = withPos;
        this.sortCounts = sortCounts;
    }

    @SuppressWarnings("null")
    public void count() throws IOException, InterruptedException {
        logger.info("Counting continuation counts of sequences.");

        Files.createDirectory(outputDir);

        Map<Pattern, Pattern> patterns = null;

        ContinuationCounterTask.setNumTasks(patterns.size());

        while (true) {
            Set<Pattern> donePatterns = new HashSet<Pattern>();

            ExecutorService executorService =
                    Executors.newFixedThreadPool(numberOfCores);

            for (Map.Entry<Pattern, Pattern> entry : patterns.entrySet()) {
                Pattern dest = entry.getKey();
                Pattern source = entry.getValue();

                if (!source.containsSkp()) {
                    donePatterns.add(dest);
                    executorService.execute(calcFromAbsolute(dest, source));
                } else if (Files.exists(outputDir.resolve(source.toString()))) {
                    donePatterns.add(dest);
                    executorService.execute(calcFromContinuation(dest, source));
                }
            }

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

            for (Pattern pattern : donePatterns) {
                patterns.remove(pattern);
            }

            if (!donePatterns.isEmpty()) {
                logger.info(
                        "End of round of calculation. Completed Patterns: {}",
                        donePatterns.size());
            } else {
                break;
            }
        }

        if (!patterns.isEmpty()) {
            StringBuilder error =
                    new StringBuilder("Could not calculate these patterns: ");
            boolean first = true;
            for (Pattern pattern : patterns.keySet()) {
                if (first) {
                    first = false;
                } else {
                    error.append(", ");
                }
                error.append(pattern);
            }
            logger.error(error.toString());
        }
    }

    @SuppressWarnings("unused")
    private Pattern getSourcePattern(Pattern pattern) {
        Pattern sourcePattern = pattern.clone();
        for (int i = sourcePattern.length() - 1; i != -1; --i) {
            PatternElem elem = sourcePattern.get(i);
            if (elem.equals(PatternElem.WSKP)) {
                sourcePattern.set(i, PatternElem.CNT);
                break;
            } else if (elem.equals(PatternElem.PSKP)) {
                sourcePattern.set(i, PatternElem.POS);
                break;
            }
        }
        return sourcePattern;
    }

    private Runnable calcFromAbsolute(Pattern dest, Pattern source) {
        Path sourceDir = inputDir.resolve(source.toString());
        Path destDir = outputDir.resolve(dest.toString());
        // TODO: buffer size calculation
        return new ContinuationCounterTask(sourceDir, destDir, wordIndex, dest,
                delimiter, 10 * 1024 * 1024, true, sortCounts);
    }

    private Runnable calcFromContinuation(Pattern dest, Pattern source) {
        Path sourceDir = outputDir.resolve(source.toString());
        Path destDir = outputDir.resolve(dest.toString());
        // TODO: buffer size calculation
        return new ContinuationCounterTask(sourceDir, destDir, wordIndex, dest,
                delimiter, 10 * 1024 * 1024, false, sortCounts);
    }

}
