/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.glmtk.logging.Logger;
import de.glmtk.util.StringUtils;

public class GlmtkPaths {
    private static final Logger LOGGER = Logger.get(GlmtkPaths.class);

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

    public static final Path CONFIG_FILE;

    static {
        USER_DIR = Paths.get(System.getProperty("user.dir"));
        GLMTK_DIR = Paths.get(System.getProperty("glmtk.dir",
                USER_DIR.toString()));
        LOG_DIR = GLMTK_DIR.resolve(Constants.LOG_DIR_NAME);
        CONFIG_FILE = GLMTK_DIR.resolve(Constants.CONFIG_FILE);
    }

    public static void logStaticPaths() {
        LOGGER.info("GlmtkPath static %s", StringUtils.repeat("-",
                80 - "GlmtkPath static ".length()));
        LOGGER.info("USER_DIR:    %s", USER_DIR);
        LOGGER.info("GLMTK_DIR:   %s", GLMTK_DIR);
        LOGGER.info("LOG_DIR:     %s", LOG_DIR);
        LOGGER.info("CONFIG_FILE: %s", CONFIG_FILE);
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

    private Path languageModelsDir;
    private Path modkneserneyDir;
    private Path modkneserneyAlphaDir;
    private Path modkneserneyLambdaDir;

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

        fillLanguageModelsDirPaths(this);

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

    private void fillLanguageModelsDirPaths(GlmtkPaths paths) {
        paths.languageModelsDir = paths.dir.resolve(Constants.LANGUAGE_MODELS_DIR_NAME);
        paths.modkneserneyDir = languageModelsDir.resolve("modkneserney");
        paths.modkneserneyAlphaDir = modkneserneyDir.resolve(Constants.ALPHA_DIR_NAME);
        paths.modkneserneyLambdaDir = modkneserneyAlphaDir.resolve(Constants.LAMBDA_DIR_NAME);
    }

    public GlmtkPaths newQueryCache(String name) {
        GlmtkPaths queryCache = new GlmtkPaths(dir);
        queryCache.root = this;
        queryCache.dir = queryCachesDir.resolve(name);
        fillCountsDirPaths(queryCache);
        fillLanguageModelsDirPaths(queryCache);
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
        LOGGER.debug("languageModelsDir      = %s", languageModelsDir);
        LOGGER.debug("modkneserneyDir        = %s", modkneserneyDir);
        LOGGER.debug("modkneserneyAlphaDir   = %s", modkneserneyAlphaDir);
        LOGGER.debug("modkneserneyLambdaDir  = %s", modkneserneyLambdaDir);
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

    public Path getLanguageModelsDir() {
        return languageModelsDir;
    }

    public Path getModkneserneyDir() {
        return modkneserneyDir;
    }

    public Path getModkneserneyAlphaDir() {
        return modkneserneyAlphaDir;
    }

    public Path getModkneserneyLambdaDir() {
        return modkneserneyLambdaDir;
    }

    public Path getQueryCachesDir() {
        return queryCachesDir;
    }

    public Path getQueriesDir() {
        return queriesDir;
    }
}
