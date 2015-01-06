package de.glmtk.exceptions;

public class CliArgumentException extends Termination {

    private static final long serialVersionUID = -8644343219016883237L;

    public CliArgumentException() {
        super();
    }

    public CliArgumentException(String message) {
        super(message);
    }

    public CliArgumentException(Throwable cause) {
        super(cause);
    }

    public CliArgumentException(String message,
                                Throwable cause) {
        super(message, cause);
    }

}
