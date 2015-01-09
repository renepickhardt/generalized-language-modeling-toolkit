package de.glmtk;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.util.StringUtils;

public class GlmtkPaths {
    private static final Logger LOGGER = LogManager.getFormatterLogger(GlmtkPaths.class);

    /**
     * The directory the user started the program from.
     */
    public static final Path USER_DIR;

    /**
     * The directory where the GLMTK bundle resides (e.g. directory where config
     * file is).
     */
    public static final Path GLMTK_DIR;

    /**
     * The directory where log files are saved.
     */
    public static final Path LOG_DIR;

    public static final Path config_FILE;

    static {
        USER_DIR = Paths.get(System.getProperty("user.dir"));
        GLMTK_DIR = Paths.get(System.getProperty("glmtk.dir",
                USER_DIR.toString()));
        LOG_DIR = GLMTK_DIR.resolve(Constants.LOG_DIR_NAME);
        config_FILE = GLMTK_DIR.resolve(Constants.CONFIG_FILE);
    }

    public static void logStaticPaths() {
        LOGGER.info("GlmtkPath static %s", StringUtils.repeat("-",
                "GlmtkPath static ".length()));
        LOGGER.info("USER_DIR:    %s");
        LOGGER.info("GLMTK_DIR:   %s");
        LOGGER.info("LOG_DIR:     %s");
        LOGGER.info("config_FILE: %s");
    }

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
