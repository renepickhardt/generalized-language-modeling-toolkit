package de.glmtk;

import java.nio.file.Path;

public class GlmtkPaths {
    private Path workingDir;

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

    private Path queriesCacheDir;
    private Path queriesDir;

    public GlmtkPaths(Path workingDir) {
        this.workingDir = workingDir;

        statusFile = workingDir.resolve(Constants.STATUS_FILE_NAME);

        trainingFile = workingDir.resolve(Constants.TRAINING_FILE_NAME);
        untaggedTrainingFile = workingDir.resolve(Constants.TRAINING_FILE_NAME
                + Constants.UNTAGGED_SUFFIX);

        countsDir = workingDir.resolve(Constants.COUNTS_DIR_NAME);
        absoluteDir = countsDir.resolve(Constants.ABSOLUTE_DIR_NAME);
        absoluteChunkedDir = countsDir.resolve(Constants.ABSOLUTE_DIR_NAME
                + Constants.CHUNKED_SUFFIX);
        continuationDir = countsDir.resolve(Constants.CONTINUATION_DIR_NAME);
        continuationChunkedDir = countsDir.resolve(Constants.CONTINUATION_DIR_NAME
                + Constants.CHUNKED_SUFFIX);
        nGramTimesFile = countsDir.resolve(Constants.NGRAMTIMES_FILE_NAME);
        lengthDistributionFile = countsDir.resolve(Constants.LENGTHDISTRIBUTION_FILE_NAME);

        queriesCacheDir = workingDir.resolve(Constants.QUERIESHACHES_DIR_NAME);
        queriesDir = workingDir.resolve(Constants.QUERIES_DIR_NAME);
    }

    public Path getWorkingDir() {
        return workingDir;
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

    public Path getQueriesCacheDir() {
        return queriesCacheDir;
    }

    public Path getQueriesDir() {
        return queriesDir;
    }
}
