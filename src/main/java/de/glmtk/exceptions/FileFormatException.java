package de.glmtk.exceptions;

import java.nio.file.Path;
import java.util.Formatter;

public class FileFormatException extends RuntimeException {

    private static final long serialVersionUID = 7551127644841955051L;

    private static String assembleMessage(Path file,
                                          String fileType,
                                          String message,
                                          Object... params) {
        try (Formatter f = new Formatter()) {
            if (fileType == null)
                f.format("Illegal file '%s'.%n", file);
            else
                f.format("Illegal %s file '%s'.%n", fileType, file);
            f.format(message, params);
            return f.toString();
        }
    }

    private static String assembleMessage(String line,
                                          Integer lineNo,
                                          Path file,
                                          String fileType,
                                          String errorType,
                                          String message,
                                          Object... params) {
        try (Formatter f = new Formatter()) {
            if (errorType == null)
                f.format("Illegal line '%d' ", lineNo);
            else
                f.format("Illegal %s on line '%d' ", errorType, lineNo);
            if (fileType == null)
                f.format(" in file '%s'.%n", file);
            else
                f.format(" in %s file '%s'.%n", fileType, file);
            f.format(message, params);
            f.format("%nLine was: '%s'.", line);
            return f.toString();
        }
    }

    public FileFormatException(Path file,
                               String fileType,
                               String message,
                               Object... params) {
        assembleMessage(file, fileType, message, params);
    }

    public FileFormatException(String line,
                               Integer lineNo,
                               Path file,
                               String fileType,
                               String errorType,
                               String message,
                               Object... params) {
        super(assembleMessage(line, lineNo, file, fileType, errorType, message,
                params));
    }

}
