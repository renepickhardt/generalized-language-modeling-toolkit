package de.glmtk;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.util.StringUtils;

public class GlmtkPaths {
    private static final Logger LOGGER = LogManager.getFormatterLogger(GlmtkPaths.class);

    private GlmtkPaths root;

    private Path dir;

    private Path statusFile;

    private Path trainingFile;
    private Path untaggedTrainingFile;

    private Path countsDir;
    private Path absoluteDir;
    private Path absoluteChunkedDir;
    private Path continuationDir;
    private Path continuationChunkedDir;
    private Path nGramTimesFile;
    private Path lengthDistributionFile;

    private Path queryCachesDir;
    private Path queriesDir;

    public GlmtkPaths(Path workingDir) {
        root = null;

        dir = workingDir;

        statusFile = dir.resolve(Constants.STATUS_FILE_NAME);

        trainingFile = dir.resolve(Constants.TRAINING_FILE_NAME);
        untaggedTrainingFile = dir.resolve(Constants.TRAINING_FILE_NAME
                + Constants.UNTAGGED_SUFFIX);

        fillCountsDirPaths(this);
        nGramTimesFile = countsDir.resolve(Constants.NGRAMTIMES_FILE_NAME);
        lengthDistributionFile = countsDir.resolve(Constants.LENGTHDISTRIBUTION_FILE_NAME);

        queryCachesDir = dir.resolve(Constants.QUERYHACHES_DIR_NAME);
        queriesDir = dir.resolve(Constants.QUERIES_DIR_NAME);
    }

    private void fillCountsDirPaths(GlmtkPaths paths) {
        paths.countsDir = paths.dir.resolve(Constants.COUNTS_DIR_NAME);
        paths.absoluteDir = paths.countsDir.resolve(Constants.ABSOLUTE_DIR_NAME);
        paths.absoluteChunkedDir = paths.countsDir.resolve(Constants.ABSOLUTE_DIR_NAME
                + Constants.CHUNKED_SUFFIX);
        paths.continuationDir = paths.countsDir.resolve(Constants.CONTINUATION_DIR_NAME);
        paths.continuationChunkedDir = paths.countsDir.resolve(Constants.CONTINUATION_DIR_NAME
                + Constants.CHUNKED_SUFFIX);
    }

    public GlmtkPaths newQueryCache(String name) {
        GlmtkPaths queryCache = new GlmtkPaths(dir);
        queryCache.root = this;
        queryCache.dir = queryCachesDir.resolve(name);
        fillCountsDirPaths(queryCache);
        return queryCache;
    }

    public void logPaths() {
        LOGGER.debug("Paths %s",
                StringUtils.repeat("-", 80 - "Paths ".length()));
        LOGGER.debug("dir                    = %s", dir);
        LOGGER.debug("statusFile             = %s", statusFile);
        LOGGER.debug("trainingFile           = %s", trainingFile);
        LOGGER.debug("untaggedTrainingFile   = %s", untaggedTrainingFile);
        LOGGER.debug("countsDir              = %s", countsDir);
        LOGGER.debug("absoluteDir            = %s", absoluteDir);
        LOGGER.debug("absoluteChunkedDir     = %s", absoluteChunkedDir);
        LOGGER.debug("continuationDir        = %s", continuationDir);
        LOGGER.debug("continuationChunkedDir = %s", continuationChunkedDir);
        LOGGER.debug("nGramTimesFile         = %s", nGramTimesFile);
        LOGGER.debug("lengthDistributionFile = %s", lengthDistributionFile);
        LOGGER.debug("queryCachesDir         = %s", queryCachesDir);
        LOGGER.debug("queriesDir             = %s", queriesDir);
    }

    public GlmtkPaths getRoot() {
        return root;
    }

    public Path getDir() {
        return dir;
    }

    public Path getStatusFile() {
        return statusFile;
    }

    public Path getTrainingFile() {
        return trainingFile;
    }

    public Path getUntaggedTrainingFile() {
        return untaggedTrainingFile;
    }

    public Path getCountsDir() {
        return countsDir;
    }

    public Path getAbsoluteDir() {
        return absoluteDir;
    }

    public Path getAbsoluteChunkedDir() {
        return absoluteChunkedDir;
    }

    public Path getContinuationDir() {
        return continuationDir;
    }

    public Path getContinuationChunkedDir() {
        return continuationChunkedDir;
    }

    public Path getNGramTimesFile() {
        return nGramTimesFile;
    }

    public Path getLengthDistributionFile() {
        return lengthDistributionFile;
    }

    public Path getQueryCachesDir() {
        return queryCachesDir;
    }

    public Path getQueriesDir() {
        return queriesDir;
    }
}
