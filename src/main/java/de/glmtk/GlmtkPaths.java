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

import static de.glmtk.Constants.ABSOLUTE_DIR_NAME;
import static de.glmtk.Constants.CHUNKED_SUFFIX;
import static de.glmtk.Constants.CONTINUATION_DIR_NAME;
import static de.glmtk.Constants.COUNTS_DIR_NAME;
import static de.glmtk.Constants.LENGTHDISTRIBUTION_FILE_NAME;
import static de.glmtk.Constants.LOG_DIR_NAME;
import static de.glmtk.Constants.NGRAMTIMES_FILE_NAME;
import static de.glmtk.Constants.QUERIES_DIR_NAME;
import static de.glmtk.Constants.QUERYHACHES_DIR_NAME;
import static de.glmtk.Constants.STATUS_FILE_NAME;
import static de.glmtk.Constants.TRAINING_FILE_NAME;
import static de.glmtk.Constants.UNTAGGED_SUFFIX;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
        LOG_DIR = GLMTK_DIR.resolve(LOG_DIR_NAME);
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
    private Path ngramTimesFile;
    private Path lengthDistributionFile;

    private Path queryCachesDir;
    private Path queriesDir;

    public GlmtkPaths(Path workingDir) {
        root = null;

        dir = workingDir;

        statusFile = dir.resolve(STATUS_FILE_NAME);

        trainingFile = dir.resolve(TRAINING_FILE_NAME);
        untaggedTrainingFile = dir.resolve(TRAINING_FILE_NAME + UNTAGGED_SUFFIX);

        fillCountsDirPaths(this);
        ngramTimesFile = countsDir.resolve(NGRAMTIMES_FILE_NAME);
        lengthDistributionFile = countsDir.resolve(LENGTHDISTRIBUTION_FILE_NAME);

        queryCachesDir = dir.resolve(QUERYHACHES_DIR_NAME);
        queriesDir = dir.resolve(QUERIES_DIR_NAME);
    }

    private void fillCountsDirPaths(GlmtkPaths paths) {
        paths.countsDir = paths.dir.resolve(COUNTS_DIR_NAME);
        paths.absoluteDir = paths.countsDir.resolve(ABSOLUTE_DIR_NAME);
        paths.absoluteChunkedDir = paths.countsDir.resolve(ABSOLUTE_DIR_NAME
                + CHUNKED_SUFFIX);
        paths.continuationDir = paths.countsDir.resolve(CONTINUATION_DIR_NAME);
        paths.continuationChunkedDir = paths.countsDir.resolve(CONTINUATION_DIR_NAME
                + CHUNKED_SUFFIX);
    }

    public GlmtkPaths newQueryCache(String name) {
        GlmtkPaths queryCache = new GlmtkPaths(dir);
        queryCache.root = this;
        queryCache.dir = queryCachesDir.resolve(name);
        fillCountsDirPaths(queryCache);
        return queryCache;
    }

    /**
     * Logs all fields store a value of type {@link Path}.
     *
     * <p>
     * Uses reflection to iterate those fields.
     */
    public void logPaths() {
        List<Entry<String, Path>> paths = new ArrayList<>();
        for (Field field : GlmtkPaths.class.getDeclaredFields())
            if (field.getType().equals(Path.class))
                try {
                    String name = field.getName();
                    Path path = (Path) field.get(this);
                    paths.add(new AbstractMap.SimpleEntry<>(name, path));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // This is just logging, we don't care for errors
                }

        int maxNameLength = 0;
        for (Entry<String, Path> entry : paths) {
            String name = entry.getKey();
            if (maxNameLength < name.length())
                maxNameLength = name.length();
        }

        LOGGER.debug("Paths %s",
                StringUtils.repeat("-", 80 - "Paths ".length()));
        for (Entry<String, Path> entry : paths) {
            String name = entry.getKey();
            Path path = entry.getValue();
            LOGGER.debug("%-" + maxNameLength + "s = %s", name, path);
        }
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
        return ngramTimesFile;
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
