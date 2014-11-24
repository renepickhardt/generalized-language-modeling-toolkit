package de.glmtk.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Util class containing various static helper methods related to nio (Java 7 io
 * api).
 */
public class NioUtils {

    /**
     * @see NioUtils#checkFile(Path, CheckFile...)
     */
    public enum CheckFile {

        EXISTS,

        IS_READABLE,

        IS_REGULAR_FILE,

        IS_DIRECTORY,

        IS_NO_DIRECTORY;

    }

    /**
     * Helper function to have multiple file checks in one function. Given a
     * {@code path} performs {@code checks} on it.
     *
     * Depending on what {@link CheckFile} values are specified, these checks
     * are performed:
     *
     * <ul>
     * <li>{@link CheckFile#EXISTS EXISTS}:
     * {@link Files#exists(Path, java.nio.file.LinkOption...)}
     * <li>{@link CheckFile#IS_READABLE IS_READABLE}:
     * {@link Files#isReadable(Path)}
     * <li>{@link CheckFile#IS_REGULAR_FILE IS_REGULAR_FILE}:
     * {@link Files#isRegularFile(Path, java.nio.file.LinkOption...)}
     * <li>{@link CheckFile#IS_DIRECTORY IS_DIRECTORY}:
     * {@link Files#isDirectory(Path, java.nio.file.LinkOption...)}
     * <li>{@link CheckFile#IS_NO_DIRECTORY IS_NO_DIRECTORY}: NOT
     * {@link Files#isDirectory(Path, java.nio.file.LinkOption...)}
     * </ul>
     *
     * @return {@code true} if all {@code checks} pass, else {@code false}.
     */
    public static boolean checkFile(Path path, CheckFile... checks) {
        for (CheckFile check : checks) {
            switch (check) {
                case EXISTS:
                    if (!Files.exists(path)) {
                        return false;
                    }
                    break;
                case IS_READABLE:
                    if (!Files.isReadable(path)) {
                        return false;
                    }
                    break;
                case IS_REGULAR_FILE:
                    if (!Files.isRegularFile(path)) {
                        return false;
                    }
                    break;
                case IS_DIRECTORY:
                    if (!Files.isDirectory(path)) {
                        return false;
                    }
                    break;
                case IS_NO_DIRECTORY:
                    if (Files.isDirectory(path)) {
                        return false;
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
        return true;
    }

    /**
     * Checks whether a given {@code dir} is empty (contains no files or other
     * directories).
     */
    public static boolean isDirEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        }
    }

}
