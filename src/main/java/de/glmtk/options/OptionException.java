package de.glmtk.options;

import static java.lang.String.format;

public class OptionException extends Exception {

    private static final long serialVersionUID = 1L;

    public OptionException() {
        super();
    }

    public OptionException(String message) {
        super(message);
    }

    public OptionException(String format,
                           Object... args) {
        super(format(format, args));
    }

    public OptionException(Throwable cause) {
        super(cause);
    }

    public OptionException(String message,
                           Throwable cause) {
        super(message, cause);
    }

}
