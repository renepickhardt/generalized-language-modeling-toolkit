package de.glmtk.exceptions;

public class FileFormatException extends RuntimeException {

    private static final long serialVersionUID = 7551127644841955051L;

    public FileFormatException() {
        super();
    }

    public FileFormatException(String message) {
        super(message);
    }

    public FileFormatException(Throwable cause) {
        super(cause);
    }

    public FileFormatException(String message,
                               Throwable cause) {
        super(message, cause);
    }

}
