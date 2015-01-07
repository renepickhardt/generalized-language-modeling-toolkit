package de.glmtk.exceptions;

public class StatusNotAccurateException extends RuntimeException {

    private static final long serialVersionUID = 37535866048450910L;

    public StatusNotAccurateException() {
        super();
    }

    public StatusNotAccurateException(String message) {
        super(message);
    }

    public StatusNotAccurateException(Throwable cause) {
        super(cause);
    }

    public StatusNotAccurateException(String message,
                                      Throwable cause) {
        super(message, cause);
    }

}
