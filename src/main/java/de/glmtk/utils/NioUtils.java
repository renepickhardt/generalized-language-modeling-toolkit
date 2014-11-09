package de.glmtk.utils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class NioUtils {

    public enum CheckFile {

        EXISTS,

        IS_READABLE,

        IS_REGULAR_FILE,

        IS_DIRECTORY,

        IS_NO_DIRECTORY;

    }

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

    public static boolean isDirEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        }
    }

}
