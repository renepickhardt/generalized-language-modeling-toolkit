package de.glmtk.exceptions;

import java.nio.file.Path;
import java.util.Formatter;

import org.yaml.snakeyaml.error.Mark;

public class FileFormatException extends RuntimeException {

    private static final long serialVersionUID = 7551127644841955051L;

    private static String assembleMessage(Path file,
                                          String fileType,
                                          String message,
                                          Object... params) {
        if (fileType == null)
            fileType = "";
        else
            fileType += ' ';

        try (Formatter f = new Formatter()) {
            f.format(message, params);
            f.format("%n In %s file '%s'.", fileType, file);
            return f.toString();
        }
    }

    private static String assembleMessage(String line,
                                          Integer lineNo,
                                          Path file,
                                          String fileType,
                                          String message,
                                          Object... params) {
        if (fileType == null)
            fileType = "";
        else
            fileType += ' ';

        try (Formatter f = new Formatter()) {
            f.format(message, params);
            f.format("%nIn %s file '%s', line %d:%n", fileType, file, lineNo);
            f.format("    " + line);
            return f.toString();
        }
    }

    private static String assembleMessage(Path file,
                                          String fileType,
                                          Mark mark,
                                          String message,
                                          Object... params) {
        int line = mark.getLine() + 1;
        int column = mark.getColumn() + 1;
        if (fileType == null)
            fileType = "";
        else
            fileType += ' ';

        try (Formatter f = new Formatter()) {
            f.format(message, params);
            f.format("%nIn %s file '%s', line %d, column %d:%n", fileType,
                    file, line, column);
            f.format(mark.get_snippet());
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
                               String message,
                               Object... params) {
        super(assembleMessage(line, lineNo, file, fileType, message, params));
    }

    public FileFormatException(Path file,
                               String fileType,
                               Mark mark,
                               String message,
                               Object... params) {
        super(assembleMessage(file, fileType, mark, message, params));
    }
}
