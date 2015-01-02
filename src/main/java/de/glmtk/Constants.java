package de.glmtk;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {

    public static final String STANDARD_WORKING_DIR_SUFFIX = ".out";

    public static final String TRAINING_FILE_NAME = "training";

    public static final String STATUS_FILE_NAME = "status";

    public static final String ABSOLUTE_DIR_NAME = "absolute";

    public static final String CONTINUATION_DIR_NAME = "continuation";

    public static final String NGRAMTIMES_FILE_NAME = "ngramtimes";

    public static final String LENGTHDISTRIBUTION_FILE_NAME =
            "lengthdistribution";

    public static final String LOG_DIR_NAME = "logs";

    public static final String ALL_LOG_FILE_NAME = "all.log";

    public static final String LOCAL_LOG_FILE_NAME = "log";

    public static final String CONFIG_LOCATION = "glmtk.conf";

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final int ORDER = 5;

    public static final long B = 1L, KB = 1024 * B, MB = 1024 * KB,
            GB = 1024 * MB;

    public static final long MAX_CHUNK_SIZE = 500 * MB;

    public static final int BUFFER_SIZE = (int) (50 * MB);

    public static final long QUEUE_TIMEOUT = 10;

    public static final String UNKOWN_POS = "UNKP";

    public static final double LOG_BASE = 10.0;

    public static final boolean DEBUG_AVERAGE_MEMORY = false;

    public static final Path TEST_RESSOURCES_DIR = Paths
            .get("src/test/resources");

}
