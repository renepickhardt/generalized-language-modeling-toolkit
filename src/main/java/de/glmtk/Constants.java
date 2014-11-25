package de.glmtk;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {

    public static final String ABSOLUTE_DIR_NAME = "absolute";

    public static final String CONTINUATION_DIR_NAME = "continuation";

    public static final int MODEL_SIZE = 5;

    public static final long B = 1L, KB = 1024 * B, MB = 1024 * KB;

    public static final long CHUNK_MAX_SIZE = 500 * KB;

    public static final long QUEUE_IDLE_TIME = 10;

    public static final String UNKOWN_POS = "UNKP";

    public static final int LOG_BASE = 10;

    public static final boolean DEBUG_AVERAGE_MEMORY = false;

    public static final Path TEST_RESSOURCES_DIR = Paths
            .get("src/test/resources");

}
