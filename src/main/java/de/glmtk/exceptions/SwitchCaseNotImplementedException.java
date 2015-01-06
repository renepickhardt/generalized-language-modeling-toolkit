package de.glmtk.exceptions;

public class SwitchCaseNotImplementedException extends RuntimeException {

    private static final long serialVersionUID = 1861334142790313101L;

    public SwitchCaseNotImplementedException() {
        super();
    }

    public SwitchCaseNotImplementedException(String message) {
        super(message);
    }

    public SwitchCaseNotImplementedException(Throwable cause) {
        super(cause);
    }

    public SwitchCaseNotImplementedException(String message,
                                             Throwable cause) {
        super(message, cause);
    }

}
