package de.glmtk.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
    public static boolean checkFile(Path path,
                                    CheckFile... checks) {
        for (CheckFile check : checks)
            switch (check) {
                case EXISTS:
                    if (!Files.exists(path))
                        return false;
                    break;
                case IS_READABLE:
                    if (!Files.isReadable(path))
                        return false;
                    break;
                case IS_REGULAR_FILE:
                    if (!Files.isRegularFile(path))
                        return false;
                    break;
                case IS_DIRECTORY:
                    if (!Files.isDirectory(path))
                        return false;
                    break;
                case IS_NO_DIRECTORY:
                    if (Files.isDirectory(path))
                        return false;
                    break;

                default:
                    throw new IllegalStateException();
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

    public static long calcFileSize(Path path) throws IOException {
        final AtomicLong size = new AtomicLong(0);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

        });
        return size.get();
    }

    public static long calcFileSize(List<Path> paths) throws IOException {
        long sum = 0;
        for (Path path : paths)
            sum += calcFileSize(path);
        return sum;
    }

    public static BufferedReader newBufferedReader(Path path,
                                                   Charset charset,
                                                   int sz) throws IOException {
        return new BufferedReader(new InputStreamReader(
                Files.newInputStream(path), charset), sz);
    }

    public static BufferedWriter newBufferedWriter(Path path,
                                                   Charset charset,
                                                   int sz) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(path), charset), sz);
    }

    public static int getNumberOfLines(Path file) throws IOException {
        try (InputStream reader = new BufferedInputStream(
                Files.newInputStream(file))) {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean endsWithoutNewLine = false;
            while ((readChars = reader.read(c)) != -1) {
                for (int i = 0; i != readChars; ++i)
                    if (c[i] == '\n')
                        ++count;
                endsWithoutNewLine = c[readChars - 1] != '\n';
            }
            if (endsWithoutNewLine)
                ++count;
            return count;
        }
    }
}
