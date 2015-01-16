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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
    public static final String WORKING_DIR_SUFFIX = ".glmtk";

    public static final String STATUS_FILE_NAME = "status";

    public static final String TRAINING_FILE_NAME = "training";

    public static final String UNTAGGED_SUFFIX = ".untagged";

    public static final String COUNTS_DIR_NAME = "counts";

    public static final String ABSOLUTE_DIR_NAME = "absolute";

    public static final String CONTINUATION_DIR_NAME = "continuation";

    public static final String CHUNKED_SUFFIX = ".chunked";

    public static final String NGRAMTIMES_FILE_NAME = "ngramtimes";

    public static final String LENGTHDISTRIBUTION_FILE_NAME = "lengthdistribution";

    public static final String QUERYHACHES_DIR_NAME = "querycaches";

    public static final String QUERIES_DIR_NAME = "queries";

    public static final String LOG_DIR_NAME = "logs";

    public static final String ALL_LOG_FILE_NAME = "all.log";

    public static final String LOCAL_LOG_FILE_NAME = "log";

    public static final String CONFIG_FILE = "glmtk.conf";

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final int ORDER = 5;

    /**
     * Size of one byte.
     *
     * Used as a unit for memory.
     *
     * @see #KiB
     * @see #MiB
     * @see #GiB
     */
    public static final long B = 1L;

    /**
     * Size of one kibibyte.
     *
     * Used as a unit for memory.
     *
     * @see #B
     * @see #MiB
     * @see #GiB
     */
    public static final long KiB = 1024 * B;

    /**
     * Size of one mebibyte.
     *
     * Used as a unit for memory.
     *
     * @see #B
     * @see #KiB
     * @see #GiB
     */
    public static final long MiB = 1024 * KiB;

    /**
     * Size of one gibibyte.
     *
     * Used as a unit for memory.
     *
     * @see #B
     * @see #KiB
     * @see #MiB
     */
    public static final long GiB = 1024 * MiB;

    /**
     * In milliseconds (ms).
     */
    public static final long MAX_IDLE_TIME = 10;

    public static final String UNKOWN_POS = "UNKP";

    public static final double LOG_BASE = 10.0;

    public static final boolean DEBUG_AVERAGE_MEMORY = false;

    public static final Path TEST_RESSOURCES_DIR = Paths.get("src/test/resources");
}
